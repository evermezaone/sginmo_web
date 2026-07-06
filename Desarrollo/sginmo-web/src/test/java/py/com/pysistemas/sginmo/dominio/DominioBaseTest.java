package py.com.pysistemas.sginmo.dominio;

import org.junit.jupiter.api.Test;
import py.com.one.core.Auditable;
import py.com.one.core.AuditoriaListener;
import py.com.one.core.UsuarioActual;
import py.com.pysistemas.sginmo.dominio.enums.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifica el dominio base (REQ-0002) contra la especificacion relevada de la BD real
 * (docs-migracion/07-datos-reales.md §3). Los nombres de los enums deben coincidir
 * EXACTAMENTE con los codigos del legado porque el ETL (REQ-0031) migra por nombre.
 */
class DominioBaseTest {

    // ── Estados (maquinas de estado doc 00) ──

    @Test
    void estadosDeOperacionSegunLegado() {
        assertArrayEquals(new String[]{"VIGENTE", "FINALIZADO"}, nombres(EstadoOperacion.values()));
    }

    @Test
    void estadosDeCuotaSegunLegado() {
        assertArrayEquals(new String[]{"PENDIENTE", "CANCELADO"}, nombres(EstadoCuota.values()));
    }

    @Test
    void estadosDeCobroSegunLegado() {
        assertArrayEquals(new String[]{"CANCELADO", "ANULADO"}, nombres(EstadoCobro.values()));
    }

    @Test
    void estadosDePropiedadSegunLegado() {
        assertArrayEquals(new String[]{"LIBRE", "OCUPADA", "VENDIDA"}, nombres(EstadoPropiedad.values()));
    }

    @Test
    void estadosDeIngresoEgresoIncluyenVencido() {
        // VENCIDO existe en el dominio real ESTADO_INGRESO_EGRESO (doc 07 §3)
        assertArrayEquals(new String[]{"PENDIENTE", "CANCELADO", "ANULADO", "VENCIDO"},
                nombres(EstadoIngresoEgreso.values()));
    }

    // ── Tipos (dominios reales doc 07 §3) ──

    @Test
    void tiposDeOperacionYCondicion() {
        assertArrayEquals(new String[]{"ALQUILER", "VENTA"}, nombres(TipoOperacion.values()));
        assertArrayEquals(new String[]{"CONTADO", "CREDITO"}, nombres(CondicionOperacion.values()));
    }

    @Test
    void tiposDeEntidadInmobiliariaSonLosSeisReales() {
        assertArrayEquals(new String[]{"EDIFICIO", "LOTEAMIENTO", "COMPLEJO", "BARRIO_CERRADO",
                "SALONES_COMERCIALES", "NO_APLICA"}, nombres(TipoEntidadInmobiliaria.values()));
    }

    @Test
    void tiposDePropiedadSonLosNueveReales() {
        assertArrayEquals(new String[]{"CASA", "DEPARTAMENTO", "DUPLEX", "LOTE", "OFICINA",
                "PIEZA", "SALONES", "ESTACIONAMIENTO", "AREA_COMUN"}, nombres(TipoPropiedad.values()));
    }

    @Test
    void personeriasUsanCodigosDelLegado() {
        // PERFIS/PERJUR son los codigos reales de la BD (no renombrar: el ETL migra por nombre)
        assertArrayEquals(new String[]{"PERFIS", "PERJUR"}, nombres(TipoPersoneria.values()));
        assertEquals("Persona Física", TipoPersoneria.PERFIS.getEtiqueta());
    }

    @Test
    void perfilesDocumentosImputacionesYVarios() {
        assertArrayEquals(new String[]{"ADMINISTRADOR", "USUARIO"}, nombres(Perfil.values()));
        assertArrayEquals(new String[]{"CI", "RUC", "DOCEX", "OTROS"}, nombres(TipoDocumentoIdentidad.values()));
        assertArrayEquals(new String[]{"ADMINISTRADOR", "ENTIDAD_INMOBILIARIA", "INQUILINO",
                "PROPIEDAD", "PROPIETARIO", "VENDEDOR"}, nombres(TipoImputacion.values()));
        assertArrayEquals(new String[]{"CUOTA", "MORA", "DESCUENTO"}, nombres(TipoDocumentoCobro.values()));
        assertArrayEquals(new String[]{"INGRESO", "EGRESO", "DESCUENTO"}, nombres(TipoItemIngresoEgreso.values()));
        assertArrayEquals(new String[]{"INGRESO", "EGRESO"}, nombres(TipoMovimiento.values()));
        assertArrayEquals(new String[]{"FIJO", "VARIABLE"}, nombres(TipoGasto.values()));
        assertArrayEquals(new String[]{"LOCAL", "EXTRANJERA"}, nombres(TipoMoneda.values()));
        assertArrayEquals(new String[]{"FINANCIACION_PROPIA", "FINANCIACION_BANCARIA"},
                nombres(TipoFinanciacion.values()));
        assertArrayEquals(new String[]{"PRIVADO", "PUBLICO"}, nombres(TipoContrato.values()));
    }

    @Test
    void todasLasEtiquetasEstanDefinidas() {
        for (TipoPropiedad t : TipoPropiedad.values()) assertFalse(t.getEtiqueta().isBlank());
        for (TipoEntidadInmobiliaria t : TipoEntidadInmobiliaria.values()) assertFalse(t.getEtiqueta().isBlank());
        for (TipoImputacion t : TipoImputacion.values()) assertFalse(t.getEtiqueta().isBlank());
    }

    // ── Listener de auditoria ──

    private static class EntidadDePrueba extends Auditable { }

    @Test
    void listenerPueblaCreacionYModificacionConFallbackSistema() {
        // Sin contenedor CDI el listener debe auditar como "sistema" (jobs/ETL/tests)
        AuditoriaListener listener = new AuditoriaListener();
        EntidadDePrueba e = new EntidadDePrueba();

        listener.alCrear(e);
        assertEquals(UsuarioActual.SISTEMA, e.getUsuarioCreacion());
        assertNotNull(e.getFechaCreacion());
        assertNull(e.getUsuarioModificacion());

        listener.alModificar(e);
        assertEquals(UsuarioActual.SISTEMA, e.getUsuarioModificacion());
        assertNotNull(e.getFechaModificacion());
        assertFalse(e.getFechaModificacion().isBefore(e.getFechaCreacion()));
    }

    private static String[] nombres(Enum<?>[] valores) {
        String[] out = new String[valores.length];
        for (int i = 0; i < valores.length; i++) out[i] = valores[i].name();
        return out;
    }
}
