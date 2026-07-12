package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.ErroresBd;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.dominio.documento.DocumentoAdjunto;
import py.com.pysistemas.sginmo.web.TenantContext;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * REQ-0053 - Gestion de adjuntos documentales. @AislarTenant + @Transactional para RLS (V34).
 * Los archivos fisicos se guardan FUERA del WAR, en una ruta configurable, con nombre UUID
 * (sin colisiones; no se confia en el nombre original). Descarga protegida por permiso + tenant.
 */
@ApplicationScoped
@AislarTenant
@Transactional
public class DocumentoAdjuntoService {

    /** Extensiones permitidas (configurable; se movera a parametros con REQ-0060). */
    private static final Set<String> EXT_PERMITIDAS = Set.of(
        "pdf", "jpg", "jpeg", "png", "gif", "doc", "docx", "xls", "xlsx", "odt", "ods", "txt", "csv");
    /** Tamano maximo por archivo (10 MB, coincide con el multipart-config de web.xml). */
    private static final long MAX_BYTES = 10L * 1024 * 1024;

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    @jakarta.inject.Inject
    private TenantContext tenant;

    /** Raiz del repositorio documental (fuera del WAR). */
    private Path baseDir() {
        String dir = System.getenv("SGINMO_ARCHIVOS_DIR");
        if (dir == null || dir.isBlank()) {
            dir = System.getProperty("user.home", ".") + "/sginmo/archivos";
        }
        return Path.of(dir);
    }

    // ── Lectura (LazyDataModel) ──

    private String base(String entidadTipo, String tipo, boolean soloActivos) {
        StringBuilder j = new StringBuilder(" FROM DocumentoAdjunto d WHERE 1=1");
        if (entidadTipo != null && !entidadTipo.isBlank()) j.append(" AND d.entidadTipo = :et");
        if (tipo != null && !tipo.isBlank()) j.append(" AND d.tipo = :tipo");
        if (soloActivos) j.append(" AND d.estado = 'ACTIVO'");
        return j.toString();
    }

    private void bind(jakarta.persistence.Query q, String entidadTipo, String tipo) {
        if (entidadTipo != null && !entidadTipo.isBlank()) q.setParameter("et", entidadTipo);
        if (tipo != null && !tipo.isBlank()) q.setParameter("tipo", tipo);
    }

    public long contar(String entidadTipo, String tipo, boolean soloActivos) {
        var q = em.createQuery("SELECT COUNT(d)" + base(entidadTipo, tipo, soloActivos), Long.class);
        bind(q, entidadTipo, tipo);
        return q.getSingleResult();
    }

    public List<DocumentoAdjunto> listar(int first, int size, String entidadTipo, String tipo, boolean soloActivos) {
        var q = em.createQuery("SELECT d" + base(entidadTipo, tipo, soloActivos)
                + " ORDER BY d.fechaCreacion DESC", DocumentoAdjunto.class);
        bind(q, entidadTipo, tipo);
        q.setFirstResult(first);
        q.setMaxResults(size);
        return q.getResultList();
    }

    // ── Escritura: guarda metadatos + escribe el archivo fisico ──

    @Transactional
    public DocumentoAdjunto guardar(DocumentoAdjunto meta, byte[] contenido, String nombreOriginal, String contentType) {
        autorizacion.exigir("documentos", "CREAR");
        Long emp = tenant.actual();
        if (emp == null || TenantContext.GLOBAL.equals(emp)) {
            throw new NegocioException("Seleccione una empresa para adjuntar documentos");
        }
        if (contenido == null || contenido.length == 0) throw new NegocioException("El archivo esta vacio");
        if (contenido.length > MAX_BYTES) throw new NegocioException("El archivo supera el tamano maximo (10 MB)");
        if (nombreOriginal == null || nombreOriginal.isBlank()) throw new NegocioException("Falta el nombre del archivo");
        if (meta.getTipo() == null || meta.getTipo().isBlank()) throw new NegocioException("El tipo es obligatorio");
        if (meta.getEntidadTipo() == null || meta.getEntidadTipo().isBlank()) meta.setEntidadTipo("GENERAL");

        String ext = extension(nombreOriginal);
        if (!EXT_PERMITIDAS.contains(ext)) {
            throw new NegocioException("Extension no permitida: ." + ext);
        }
        String fisico = UUID.randomUUID().toString().replace("-", "") + "." + ext;
        try {
            Path dir = baseDir().resolve(String.valueOf(emp));
            Files.createDirectories(dir);
            Files.write(dir.resolve(fisico), contenido);
        } catch (IOException e) {
            throw new NegocioException("No se pudo guardar el archivo en el servidor");
        }

        meta.setTenant(emp);
        meta.setNombreOriginal(nombreOriginal);
        meta.setNombreFisico(fisico);
        meta.setContentType(contentType);
        meta.setTamano(contenido.length);
        meta.setEstado("ACTIVO");
        try {
            em.persist(meta);
            em.flush();
            return meta;
        } catch (RuntimeException ex) {
            throw ErroresBd.traducir(ex);
        }
    }

    /** Lee el contenido de un adjunto, con permiso VER y aislamiento por tenant (RLS). */
    public byte[] leer(Long id) {
        autorizacion.exigir("documentos", "VER");
        DocumentoAdjunto d = em.find(DocumentoAdjunto.class, id);   // RLS: solo del tenant actual
        if (d == null) throw new NegocioException("El documento no existe o no pertenece a su empresa");
        try {
            Path p = baseDir().resolve(String.valueOf(d.getTenant())).resolve(d.getNombreFisico());
            if (!Files.isReadable(p)) throw new NegocioException("El archivo fisico no esta disponible");
            return Files.readAllBytes(p);
        } catch (IOException e) {
            throw new NegocioException("No se pudo leer el archivo");
        }
    }

    /** Baja logica del adjunto (conserva el archivo fisico y el historial). */
    @Transactional
    public void baja(Long id) {
        autorizacion.exigir("documentos", "INACTIVAR");
        DocumentoAdjunto d = em.find(DocumentoAdjunto.class, id);
        if (d == null) throw new NegocioException("El documento no existe");
        d.setEstado("INACTIVO");
        em.merge(d);
    }

    private static String extension(String nombre) {
        int p = nombre.lastIndexOf('.');
        return p >= 0 && p < nombre.length() - 1
                ? nombre.substring(p + 1).toLowerCase(Locale.ROOT) : "";
    }
}
