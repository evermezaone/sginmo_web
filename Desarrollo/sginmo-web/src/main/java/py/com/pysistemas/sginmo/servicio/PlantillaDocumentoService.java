package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.core.NegocioException;
import py.com.one.security.servicio.Autorizacion;
import py.com.pysistemas.sginmo.dominio.documento.DocumentoGenerado;
import py.com.pysistemas.sginmo.dominio.documento.PlantillaDocumento;

import java.math.BigDecimal;
import java.security.MessageDigest;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Maestro y generacion documental desde plantillas validadas (REQ-0041). */
@ApplicationScoped
@AislarTenant
@Transactional
public class PlantillaDocumentoService {

    public record DocumentoPdf(byte[] contenido, String nombreArchivo) { }

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @Inject
    private py.com.pysistemas.sginmo.web.TenantContext tenant;

    @Inject
    private Autorizacion autorizacion;

    @Inject
    private PdfService pdf;

    private final PlantillaDocumentoMotor motor = new PlantillaDocumentoMotor();

    private static final Long GLOBAL = -1L;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DecimalFormat GS;
    static {
        var s = new DecimalFormatSymbols(Locale.forLanguageTag("es-PY"));
        s.setGroupingSeparator('.');
        GS = new DecimalFormat("#,##0", s);
    }

    public List<PlantillaDocumentoMotor.Variable> variablesDisponibles() {
        return motor.variablesDisponibles();
    }

    public List<PlantillaDocumento> listar(String filtro) {
        autorizacion.exigir("plantillas-documentos", "VER");
        String f = filtro == null ? "" : filtro.trim().toLowerCase();
        return em.createQuery(
                "SELECT p FROM PlantillaDocumento p WHERE (p.tenant = -1 OR p.tenant = :t)"
                + " AND (:f = '' OR lower(p.codigo) LIKE :like OR lower(p.descripcion) LIKE :like)"
                + " ORDER BY p.tipo, p.descripcion", PlantillaDocumento.class)
            .setParameter("t", tenant.actual())
            .setParameter("f", f)
            .setParameter("like", "%" + f + "%")
            .getResultList();
    }

    public PlantillaDocumento nueva(String tipo) {
        var p = new PlantillaDocumento();
        p.setTipo(tipo == null ? "CONTRATO" : tipo);
        p.setTenant(tenant.esSuperadmin() ? GLOBAL : tenant.actual());
        p.setFormatoCuerpo("TEXTO");
        p.setEstado("ACTIVO");
        p.setCuerpo("");
        return p;
    }

    public PlantillaDocumento guardar(PlantillaDocumento p) {
        autorizacion.exigir("plantillas-documentos", p.getId() == null ? "CREAR" : "EDITAR");
        validarPlantilla(p);
        Long actorTenant = tenant.actual();
        boolean sa = tenant.esSuperadmin();
        if (p.getId() == null) {
            if (p.getTenant() == null) p.setTenant(sa ? GLOBAL : actorTenant);
            if (!sa && !actorTenant.equals(p.getTenant())) {
                throw new NegocioException("No puede crear plantillas para otra empresa");
            }
            em.persist(p);
            return p;
        }
        PlantillaDocumento enBd = em.find(PlantillaDocumento.class, p.getId());
        if (enBd == null) throw new NegocioException("La plantilla no existe");
        if (!sa && !actorTenant.equals(enBd.getTenant())) {
            throw new NegocioException("No puede editar plantillas de otra empresa");
        }
        if (!sa && GLOBAL.equals(enBd.getTenant())) {
            throw new NegocioException("Las plantillas globales solo las edita SUPERADMIN");
        }
        p.setTenant(enBd.getTenant());
        p.setVersionPlantilla(enBd.getVersionPlantilla() == null ? 1 : enBd.getVersionPlantilla() + 1);
        return em.merge(p);
    }

    public void cambiarEstado(Long id, String estado) {
        autorizacion.exigir("plantillas-documentos", "ACTIVO".equals(estado) ? "REACTIVAR" : "INACTIVAR");
        PlantillaDocumento p = em.find(PlantillaDocumento.class, id);
        if (p == null) throw new NegocioException("La plantilla no existe");
        if (!tenant.esSuperadmin() && !tenant.actual().equals(p.getTenant())) {
            throw new NegocioException("No puede cambiar plantillas de otra empresa");
        }
        p.setEstado(estado);
    }

    public DocumentoPdf contrato(Long operacionId, String usuario) {
        autorizacion.exigir("operaciones", "GENERAR_CONTRATO");
        DatosOperacion op = datosOperacion(operacionId);
        PlantillaDocumento plantilla = resolverPlantilla("CONTRATO", op);
        Map<String, String> valores = valoresOperacion(op);
        String cuerpo = motor.render(plantilla.getCuerpo(), valores);
        var r = pdf.iniciar(op.empresaRazon, "CONTRATO - Operacion " + op.operacion, usuario, op.clienteNombre);
        pdf.parrafos(r, cuerpo);
        byte[] bytes = pdf.cerrar(r);
        String nombre = "contrato-" + op.operacion + ".pdf";
        registrar(op.tenant, operacionId, null, "CONTRATO", plantilla, nombre, bytes);
        return new DocumentoPdf(bytes, nombre);
    }

    public DocumentoPdf pagare(Long operacionId, Long cuotaId, String usuario) {
        autorizacion.exigir("operaciones", "GENERAR_PAGARE");
        DatosOperacion op = datosOperacion(operacionId);
        DatosCuota cuota = datosCuota(operacionId, cuotaId);
        PlantillaDocumento plantilla = resolverPlantilla("PAGARE", op);
        Map<String, String> valores = valoresOperacion(op);
        valores.putAll(valoresCuota(cuota, op));
        String cuerpo = motor.render(plantilla.getCuerpo(), valores);
        var r = pdf.iniciar(op.empresaRazon, "PAGARE - Cuota " + cuota.numero, usuario, op.clienteNombre);
        pdf.parrafos(r, cuerpo);
        byte[] bytes = pdf.cerrar(r);
        String nombre = "pagare-op" + op.operacion + "-cuota" + cuota.numero + ".pdf";
        registrar(op.tenant, operacionId, cuota.id, "PAGARE", plantilla, nombre, bytes);
        return new DocumentoPdf(bytes, nombre);
    }

    public DocumentoPdf pagares(Long operacionId, List<Long> cuotasIds, String usuario) {
        autorizacion.exigir("operaciones", "GENERAR_PAGARE");
        DatosOperacion op = datosOperacion(operacionId);
        List<DatosCuota> cuotas = cuotasParaLote(operacionId, cuotasIds);
        if (cuotas.isEmpty()) throw new NegocioException("No hay cuotas pendientes o seleccionadas para generar pagares");
        PlantillaDocumento plantilla = resolverPlantilla("PAGARE", op);
        var r = pdf.iniciar(op.empresaRazon, "PAGARES - Operacion " + op.operacion, usuario, op.clienteNombre);
        boolean primera = true;
        for (DatosCuota cuota : cuotas) {
            if (!primera) pdf.nuevaPagina(r);
            primera = false;
            Map<String, String> valores = valoresOperacion(op);
            valores.putAll(valoresCuota(cuota, op));
            pdf.parrafos(r, motor.render(plantilla.getCuerpo(), valores));
        }
        byte[] bytes = pdf.cerrar(r);
        String nombre = "pagares-op" + op.operacion + ".pdf";
        registrar(op.tenant, operacionId, null, "PAGARE", plantilla, nombre, bytes);
        return new DocumentoPdf(bytes, nombre);
    }

    private void validarPlantilla(PlantillaDocumento p) {
        if (p.getCodigo() == null || p.getCodigo().isBlank()) throw new NegocioException("El codigo es obligatorio");
        if (p.getDescripcion() == null || p.getDescripcion().isBlank()) throw new NegocioException("La descripcion es obligatoria");
        if (!List.of("CONTRATO", "PAGARE").contains(p.getTipo())) throw new NegocioException("Tipo de plantilla invalido");
        if (p.getCuerpo() == null || p.getCuerpo().isBlank()) throw new NegocioException("El cuerpo de la plantilla es obligatorio");
        motor.validar(p.getCuerpo(), motor.codigosPermitidos());
    }

    private PlantillaDocumento resolverPlantilla(String tipo, DatosOperacion op) {
        var lista = em.createQuery(
                "SELECT p FROM PlantillaDocumento p WHERE p.tipo = :tipo AND p.estado = 'ACTIVO'"
                + " AND (p.tenant = -1 OR p.tenant = :t)"
                + " AND (p.tipoOperacion IS NULL OR p.tipoOperacion = :to)"
                + " AND (p.tipoContrato IS NULL OR p.tipoContrato = :tc)"
                + " ORDER BY CASE WHEN p.tenant = :t THEN 0 ELSE 1 END,"
                + " CASE WHEN p.tipoOperacion IS NOT NULL THEN 0 ELSE 1 END,"
                + " CASE WHEN p.tipoContrato IS NOT NULL THEN 0 ELSE 1 END,"
                + " p.descripcion", PlantillaDocumento.class)
            .setParameter("tipo", tipo)
            .setParameter("t", op.tenant)
            .setParameter("to", op.tipoOperacion)
            .setParameter("tc", op.tipoContrato)
            .setMaxResults(1)
            .getResultList();
        if (lista.isEmpty()) throw new NegocioException("No hay plantilla activa para " + tipo);
        return lista.get(0);
    }

    private DatosOperacion datosOperacion(Long operacionId) {
        if (operacionId == null) throw new NegocioException("Falta la operacion");
        List<Object[]> filas = em.createNativeQuery(
            "SELECT o.operacion, o.tenant, o.tipo_operacion, o.fecha_inicio_contrato, o.fecha_fin_contrato,"
            + " o.plazo, o.monto_total_operacion, o.precio, o.garantia, o.tipo_contrato,"
            + " cli.nombre, cli.numero_documento, a.nombre, COALESCE(a.direccion,''),"
            + " COALESCE(tc.descripcion,''), emp.razon_social, pe.numero_documento, COALESCE(s.descripcion,'')"
            + " FROM operacion o"
            + " JOIN persona cli ON cli.persona = o.cliente"
            + " JOIN activo a ON a.activo = o.activo"
            + " JOIN persona_juridica emp ON emp.persona = o.tenant"
            + " JOIN persona pe ON pe.persona = emp.persona"
            + " LEFT JOIN sucursal s ON s.sucursal = o.sucursal"
            + " LEFT JOIN entidad tc ON tc.entidad = o.tipo_contrato"
            + " WHERE o.operacion = :op AND o.tenant = :t")
            .setParameter("op", operacionId).setParameter("t", tenant.actual())
            .getResultList();
        if (filas.isEmpty()) throw new NegocioException("La operacion no existe o pertenece a otra empresa");
        Object[] r = filas.get(0);
        return new DatosOperacion(
            num(r[0]).longValue(), num(r[1]).longValue(), txt(r[2]), fecha(r[3]), fecha(r[4]),
            r[5] == null ? null : num(r[5]).intValue(), bd(r[6]), bd(r[7]), bd(r[8]),
            r[9] == null ? null : num(r[9]).longValue(), txt(r[10]), txt(r[11]), txt(r[12]), txt(r[13]),
            txt(r[14]), txt(r[15]), txt(r[16]), txt(r[17])
        );
    }

    @SuppressWarnings("unchecked")
    private DatosCuota datosCuota(Long operacionId, Long cuotaId) {
        List<Object[]> filas = em.createNativeQuery(
            "SELECT cronograma_cuota, numero_cuota, fecha_vencimiento, monto, estado"
            + " FROM cronograma_cuota WHERE cronograma_cuota = :c AND operacion = :op")
            .setParameter("c", cuotaId).setParameter("op", operacionId).getResultList();
        if (filas.isEmpty()) throw new NegocioException("La cuota no existe para la operacion");
        return cuota(filas.get(0));
    }

    private List<DatosCuota> cuotasParaLote(Long operacionId, List<Long> cuotasIds) {
        boolean seleccionadas = cuotasIds != null && !cuotasIds.isEmpty();
        if (seleccionadas) {
            return em.createQuery(
                    "SELECT c FROM CronogramaCuota c WHERE c.operacion = :op AND c.id IN :ids ORDER BY c.numeroCuota",
                    py.com.pysistemas.sginmo.dominio.operacion.CronogramaCuota.class)
                .setParameter("op", operacionId)
                .setParameter("ids", cuotasIds)
                .getResultList().stream()
                .map(c -> new DatosCuota(c.getId(), c.getNumeroCuota(), c.getFechaVencimiento(), c.getMonto(), c.getEstado()))
                .toList();
        }
        return em.createQuery(
                "SELECT c FROM CronogramaCuota c WHERE c.operacion = :op AND c.estado = 'PENDIENTE' ORDER BY c.numeroCuota",
                py.com.pysistemas.sginmo.dominio.operacion.CronogramaCuota.class)
            .setParameter("op", operacionId)
            .getResultList().stream()
            .map(c -> new DatosCuota(c.getId(), c.getNumeroCuota(), c.getFechaVencimiento(), c.getMonto(), c.getEstado()))
            .toList();
    }

    private DatosCuota cuota(Object[] r) {
        return new DatosCuota(num(r[0]).longValue(), num(r[1]).intValue(), fecha(r[2]), bd(r[3]), txt(r[4]));
    }

    private Map<String, String> valoresOperacion(DatosOperacion op) {
        var v = new LinkedHashMap<String, String>();
        v.put("empresa.razon_social", op.empresaRazon);
        v.put("empresa.ruc", op.empresaRuc);
        v.put("sucursal.descripcion", op.sucursal);
        v.put("operacion.numero", String.valueOf(op.operacion));
        v.put("operacion.tipo", op.tipoOperacion);
        v.put("operacion.fecha_inicio", fmt(op.inicio));
        v.put("operacion.fecha_fin", fmt(op.fin));
        v.put("operacion.plazo", op.plazo == null ? "" : String.valueOf(op.plazo));
        v.put("operacion.monto_total", gs(op.montoTotal));
        v.put("operacion.monto_total_letras", NumeroLetras.guaranies(op.montoTotal));
        v.put("operacion.precio", gs(op.precio));
        v.put("operacion.garantia", gs(op.garantia));
        v.put("cliente.nombre", op.clienteNombre);
        v.put("cliente.documento", op.clienteDocumento);
        v.put("deudor.nombre", op.clienteNombre);
        v.put("deudor.documento", op.clienteDocumento);
        v.put("acreedor.nombre", op.empresaRazon);
        v.put("activo.descripcion", op.activoNombre);
        v.put("activo.direccion", op.activoDireccion);
        v.put("tipo_contrato.descripcion", op.tipoContratoDescripcion);
        return v;
    }

    private Map<String, String> valoresCuota(DatosCuota c, DatosOperacion op) {
        var v = new LinkedHashMap<String, String>();
        v.put("cuota.numero", String.valueOf(c.numero));
        v.put("cuota.vencimiento", fmt(c.vencimiento));
        v.put("cuota.monto", gs(c.monto));
        v.put("cuota.monto_letras", NumeroLetras.guaranies(c.monto));
        return v;
    }

    private void registrar(Long tenant, Long operacion, Long cuota, String tipo, PlantillaDocumento p, String nombre, byte[] bytes) {
        var d = new DocumentoGenerado();
        d.setTenant(tenant);
        d.setOperacion(operacion);
        d.setCronogramaCuota(cuota);
        d.setTipo(tipo);
        d.setPlantillaDocumento(p.getId());
        d.setVersionPlantilla(p.getVersionPlantilla());
        d.setNombreArchivo(nombre);
        d.setHashContenido(hash(bytes));
        em.persist(d);
    }

    private String hash(byte[] bytes) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));
        } catch (Exception e) {
            throw new NegocioException("No se pudo calcular hash del documento");
        }
    }

    private static String fmt(LocalDate f) { return f == null ? "" : f.format(FECHA); }
    private static String gs(BigDecimal n) { return n == null ? "0" : GS.format(n); }
    private static String txt(Object o) { return o == null ? "" : o.toString(); }
    private static Number num(Object o) { return (Number) o; }
    private static BigDecimal bd(Object o) { return o == null ? BigDecimal.ZERO : (BigDecimal) o; }
    private static LocalDate fecha(Object o) {
        if (o == null) return null;
        if (o instanceof LocalDate l) return l;
        if (o instanceof java.sql.Date d) return d.toLocalDate();
        return LocalDate.parse(o.toString());
    }

    private record DatosOperacion(Long operacion, Long tenant, String tipoOperacion, LocalDate inicio, LocalDate fin,
                                  Integer plazo, BigDecimal montoTotal, BigDecimal precio, BigDecimal garantia,
                                  Long tipoContrato, String clienteNombre, String clienteDocumento,
                                  String activoNombre, String activoDireccion, String tipoContratoDescripcion,
                                  String empresaRazon, String empresaRuc, String sucursal) { }

    private record DatosCuota(Long id, Integer numero, LocalDate vencimiento, BigDecimal monto, String estado) { }
}
