package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.dominio.documento.DocumentoGenerado;
import py.com.pysistemas.sginmo.web.TenantContext;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

/**
 * REQ-0054 - Estado documental de documentos generados (contratos/pagares). @AislarTenant
 * para RLS (V29). El estado operativo es independiente del archivo fisico; anular no borra
 * el historial ni el archivo. Permisos separados para cambiar estado (EDITAR) y anular (INACTIVAR).
 */
@ApplicationScoped
@AislarTenant
@Transactional
public class DocumentoGeneradoService {

    /** Estados que cuentan como "pendiente de firma". */
    private static final Set<String> PENDIENTE_FIRMA = Set.of("GENERADO", "IMPRESO", "ENVIADO", "OBSERVADO");
    private static final Set<String> ESTADOS = Set.of(
        "GENERADO", "IMPRESO", "ENVIADO", "FIRMADO", "OBSERVADO", "ANULADO", "ARCHIVADO");

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;

    @jakarta.inject.Inject
    private TenantContext tenant;

    @jakarta.inject.Inject
    private py.com.one.security.web.SesionUsuario sesion;

    private String base(String estado, boolean soloPendientesFirma) {
        StringBuilder j = new StringBuilder(" FROM DocumentoGenerado d WHERE 1=1");
        if (estado != null && !estado.isBlank()) j.append(" AND d.estadoDocumental = :estado");
        if (soloPendientesFirma) j.append(" AND d.estadoDocumental IN ('GENERADO','IMPRESO','ENVIADO','OBSERVADO')");
        return j.toString();
    }

    public long contar(String estado, boolean soloPendientesFirma) {
        var q = em.createQuery("SELECT COUNT(d)" + base(estado, soloPendientesFirma), Long.class);
        if (estado != null && !estado.isBlank()) q.setParameter("estado", estado);
        return q.getSingleResult();
    }

    public List<DocumentoGenerado> listar(int first, int size, String estado, boolean soloPendientesFirma) {
        var q = em.createQuery("SELECT d" + base(estado, soloPendientesFirma)
                + " ORDER BY d.fechaCreacion DESC", DocumentoGenerado.class);
        if (estado != null && !estado.isBlank()) q.setParameter("estado", estado);
        q.setFirstResult(first);
        q.setMaxResults(size);
        return q.getResultList();
    }

    /** Avanza el estado documental y sella la fecha correspondiente. Permiso EDITAR. */
    @Transactional
    public void cambiarEstado(Long id, String nuevoEstado) {
        autorizacion.exigir("documentos-generados", "EDITAR");
        if (!ESTADOS.contains(nuevoEstado)) throw new NegocioException("Estado invalido: " + nuevoEstado);
        DocumentoGenerado d = requerir(id);
        if ("ANULADO".equals(d.getEstadoDocumental())) {
            throw new NegocioException("El documento esta anulado; no se puede cambiar su estado");
        }
        LocalDateTime ahora = LocalDateTime.now();
        switch (nuevoEstado) {
            case "IMPRESO"  -> d.setFechaImpresion(ahora);
            case "ENVIADO"  -> d.setFechaEnvio(ahora);
            case "FIRMADO"  -> d.setFechaFirma(ahora);
            case "ARCHIVADO"-> d.setFechaArchivo(ahora);
            default -> { /* GENERADO/OBSERVADO: sin fecha propia */ }
        }
        d.setEstadoDocumental(nuevoEstado);
        em.merge(d);
    }

    /** Registra la version firmada escaneada (un adjunto de REQ-0053) y marca FIRMADO. Permiso EDITAR. */
    @Transactional
    public void registrarFirma(Long id, Long adjuntoFirmadoId) {
        autorizacion.exigir("documentos-generados", "EDITAR");
        DocumentoGenerado d = requerir(id);
        if ("ANULADO".equals(d.getEstadoDocumental())) {
            throw new NegocioException("El documento esta anulado");
        }
        d.setAdjuntoFirmado(adjuntoFirmadoId);
        d.setEstadoDocumental("FIRMADO");
        d.setFechaFirma(LocalDateTime.now());
        em.merge(d);
    }

    /** Anula el documento (no borra archivo ni historial). Registra usuario y motivo. Permiso INACTIVAR. */
    @Transactional
    public void anular(Long id, String motivo) {
        autorizacion.exigir("documentos-generados", "INACTIVAR");
        if (motivo == null || motivo.isBlank()) throw new NegocioException("El motivo de anulacion es obligatorio");
        DocumentoGenerado d = requerir(id);
        d.setEstadoDocumental("ANULADO");
        d.setMotivoAnulacion(motivo);
        d.setUsuarioAnulacion(sesion.codigoUsuario());
        d.setFechaAnulacion(LocalDateTime.now());
        em.merge(d);
    }

    private DocumentoGenerado requerir(Long id) {
        DocumentoGenerado d = em.find(DocumentoGenerado.class, id);   // RLS: solo del tenant actual
        if (d == null) throw new NegocioException("El documento no existe o no pertenece a su empresa");
        return d;
    }
}
