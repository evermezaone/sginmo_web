package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.NegocioException;
import py.com.pysistemas.sginmo.dominio.catalogo.ParametroSistema;
import py.com.pysistemas.sginmo.web.TenantContext;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * REQ-0061 - Importacion asistida CSV (UTF-8). Framework generico: vista previa con validacion por
 * fila, confirmacion ATOMICA (no inserta si hay errores) e historial. @AislarTenant. Las validaciones
 * reutilizan los servicios de negocio (ej.: ParametroService). XLSX: diferido (requiere Apache POI).
 */
@ApplicationScoped
@AislarTenant
@Transactional
public class ImportacionService {

    private static final Set<String> TIPOS_PARAM = Set.of("STRING", "ENTERO", "DECIMAL", "BOOLEAN", "FECHA");

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @jakarta.inject.Inject
    private py.com.one.security.servicio.Autorizacion autorizacion;
    @jakarta.inject.Inject
    private py.com.one.security.web.SesionUsuario sesion;
    @jakarta.inject.Inject
    private TenantContext tenant;
    @jakarta.inject.Inject
    private ParametroService parametroService;

    /** Plantilla CSV (encabezado) por tipo. Requiere permiso EXPORTAR. */
    public byte[] plantilla(String tipo) {
        autorizacion.exigir("importacion", "EXPORTAR");
        String header = switch (tipo) {
            case "PARAMETRO" -> "clave,valor,descripcion,grupo,tipo\n";
            default -> throw new NegocioException("Tipo de importacion no soportado: " + tipo);
        };
        return header.getBytes(StandardCharsets.UTF_8);
    }

    /** Vista previa con validacion por fila (no persiste). Requiere permiso VER. */
    public List<FilaPreview> preview(String tipo, byte[] csv) {
        autorizacion.exigir("importacion", "VER");
        List<String[]> filas = parse(csv);
        List<FilaPreview> out = new ArrayList<>();
        int nro = 0;
        for (String[] f : filas) {
            nro++;
            FilaPreview fp = new FilaPreview();
            fp.numero = nro;
            fp.valores = f;
            fp.error = validar(tipo, f);
            fp.valido = fp.error == null;
            out.add(fp);
        }
        return out;
    }

    /** Confirma la importacion: ATOMICA (si hay filas invalidas, no inserta nada). Registra historial. */
    @Transactional
    public Resultado importar(String tipo, String archivo, byte[] csv) {
        autorizacion.exigir("importacion", "CREAR");
        Long emp = tenant.actual();
        if (emp == null || TenantContext.GLOBAL.equals(emp)) throw new NegocioException("Seleccione una empresa");

        List<FilaPreview> filas = preview(tipo, csv);
        Resultado r = new Resultado();
        r.filasTotal = filas.size();
        for (FilaPreview fp : filas) {
            if (fp.valido) r.filasValidas++;
            else { r.filasError++; r.errores.add("Fila " + fp.numero + ": " + fp.error); }
        }

        if (r.filasError == 0 && r.filasTotal > 0) {
            for (FilaPreview fp : filas) insertar(tipo, fp.valores, emp);
            r.ok = true;
        } else {
            r.ok = false;   // politica atomica: no se inserta nada si hay errores
        }
        registrarHistorial(emp, tipo, archivo, r);
        return r;
    }

    /** Historial de importaciones del tenant (cap 100). */
    public List<Object[]> historial() {
        Long emp = tenant.actual();
        if (emp == null || TenantContext.GLOBAL.equals(emp)) return new ArrayList<>();
        @SuppressWarnings("unchecked")
        List<Object[]> h = em.createNativeQuery(
            "SELECT fecha, tipo, archivo, usuario, filas_validas, filas_error, resultado"
          + " FROM importacion WHERE tenant=:t ORDER BY importacion DESC")
            .setParameter("t", emp).setMaxResults(100).getResultList();
        return h;
    }

    // ── Validacion / insercion por tipo ──

    private String validar(String tipo, String[] f) {
        if ("PARAMETRO".equals(tipo)) {
            if (f.length < 2) return "faltan columnas (minimo clave,valor)";
            if (f[0] == null || f[0].isBlank()) return "clave vacia";
            String t = f.length >= 5 ? f[4].trim().toUpperCase() : "STRING";
            if (!t.isBlank() && !TIPOS_PARAM.contains(t)) return "tipo invalido: " + t;
            return null;
        }
        return "tipo no soportado";
    }

    private void insertar(String tipo, String[] f, Long emp) {
        if ("PARAMETRO".equals(tipo)) {
            ParametroSistema p = new ParametroSistema();
            p.setTenant(emp);   // se importa para la empresa actual (override del default global)
            p.setClave(f[0].trim());
            p.setValor(f.length >= 2 ? f[1] : "");
            p.setDescripcion(f.length >= 3 && !f[2].isBlank() ? f[2] : f[0].trim());
            if (f.length >= 4) p.setGrupo(f[3].isBlank() ? null : f[3].trim());
            if (f.length >= 5 && !f[4].isBlank()) p.setTipo(f[4].trim().toUpperCase());
            parametroService.guardar(p, true);   // reutiliza validacion/persistencia de negocio
        }
    }

    private void registrarHistorial(Long emp, String tipo, String archivo, Resultado r) {
        String detalle = String.join(" | ", r.errores);
        if (detalle.length() > 2000) detalle = detalle.substring(0, 1997) + "...";
        em.createNativeQuery(
            "INSERT INTO importacion (tenant, tipo, archivo, usuario, filas_total, filas_validas, filas_error, resultado, detalle)"
          + " VALUES (:t,:tipo,:arch,:usr,:ft,:fv,:fe,:res,:det)")
            .setParameter("t", emp).setParameter("tipo", tipo).setParameter("arch", archivo)
            .setParameter("usr", sesion.codigoUsuario()).setParameter("ft", r.filasTotal)
            .setParameter("fv", r.filasValidas).setParameter("fe", r.filasError)
            .setParameter("res", r.ok ? "OK" : "FALLIDO").setParameter("det", detalle)
            .executeUpdate();
    }

    /** Parser CSV minimo UTF-8: salta encabezado y lineas vacias; separa por coma (sin comas embebidas). */
    private List<String[]> parse(byte[] csv) {
        String texto = new String(csv, StandardCharsets.UTF_8).replace("﻿", "");
        List<String[]> out = new ArrayList<>();
        String[] lineas = texto.split("\r?\n");
        for (int i = 1; i < lineas.length; i++) {   // i=1: salta encabezado
            String ln = lineas[i];
            if (ln == null || ln.isBlank()) continue;
            String[] cols = ln.split(",", -1);
            for (int j = 0; j < cols.length; j++) cols[j] = cols[j].trim();
            out.add(cols);
        }
        return out;
    }

    public static class FilaPreview {
        public int numero;
        public String[] valores;
        public boolean valido;
        public String error;
        public int getNumero() { return numero; }
        public String[] getValores() { return valores; }
        public boolean isValido() { return valido; }
        public String getError() { return error; }
        public String getResumen() { return valores == null ? "" : String.join(" | ", valores); }
    }

    public static class Resultado {
        public int filasTotal, filasValidas, filasError;
        public boolean ok;
        public List<String> errores = new ArrayList<>();
        public int getFilasTotal() { return filasTotal; }
        public int getFilasValidas() { return filasValidas; }
        public int getFilasError() { return filasError; }
        public boolean isOk() { return ok; }
        public List<String> getErrores() { return errores; }
    }
}
