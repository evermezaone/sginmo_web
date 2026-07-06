package py.com.one.security.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.faces.context.FacesContext;
import jakarta.inject.Inject;
import org.primefaces.component.column.Column;
import org.primefaces.component.datatable.DataTable;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;
import py.com.one.security.web.SesionUsuario;

/**
 * "Mi vista" reutilizable (obs 206 de Codex): guarda/aplica por usuario y pantalla las
 * columnas visibles, filas por pagina, orden y filtro global de la dataTable estandar
 * (id frmLista:tabla). Cualquier problema con el JSON guardado se ignora: una vista
 * corrupta jamas rompe la pantalla.
 */
@ApplicationScoped
public class VistaUsuario {

    private static final String CLAVE = "mi_vista";

    @Inject
    private PreferenciaService preferencias;

    /** Guarda la vista actual; devuelve false si no hay tabla o sesion. */
    public boolean guardar(String pantalla, String filtroGlobal) {
        var dt = tabla();
        Long usuario = usuarioId();
        if (dt == null || usuario == null) {
            return false;
        }
        var json = jakarta.json.Json.createObjectBuilder();
        var columnas = jakarta.json.Json.createObjectBuilder();
        for (var col : dt.getColumns()) {
            if (col instanceof Column && ((Column) col).isRendered() && ((Column) col).isToggleable()) {
                Column c = (Column) col;
                columnas.add(c.getHeaderText(), c.isVisible());
            }
        }
        json.add("columnas", columnas);
        json.add("filas", dt.getRows());
        json.add("filtroGlobal", filtroGlobal == null ? "" : filtroGlobal);
        dt.getSortByAsMap().values().stream()
            .filter(SortMeta::isActive).findFirst()
            .ifPresent(s -> json.add("orden", s.getField())
                                .add("asc", s.getOrder() != SortOrder.DESCENDING));
        preferencias.guardar(usuario, pantalla, CLAVE, json.build().toString());
        return true;
    }

    public void quitar(String pantalla) {
        Long usuario = usuarioId();
        if (usuario != null) {
            preferencias.eliminar(usuario, pantalla, CLAVE);
        }
    }

    /**
     * Aplica la vista guardada a la tabla; devuelve el filtro global guardado
     * (o null si no hay vista) para que el bean lo asigne a su campo.
     */
    public String aplicar(String pantalla) {
        try {
            Long usuario = usuarioId();
            if (usuario == null) {
                return null;
            }
            String json = preferencias.leer(usuario, pantalla, CLAVE);
            var dt = tabla();
            if (json == null || dt == null) {
                return null;
            }
            try (var lector = jakarta.json.Json.createReader(new java.io.StringReader(json))) {
                var vista = lector.readObject();
                var columnas = vista.getJsonObject("columnas");
                if (columnas != null) {
                    for (var col : dt.getColumns()) {
                        if (col instanceof Column && columnas.containsKey(((Column) col).getHeaderText())) {
                            Column c = (Column) col;
                            c.setVisible(columnas.getBoolean(c.getHeaderText()));
                        }
                    }
                }
                dt.setRows(vista.getInt("filas", 10));
                if (vista.containsKey("orden")) {
                    var orden = SortMeta.builder()
                        .field(vista.getString("orden"))
                        .order(vista.getBoolean("asc", true) ? SortOrder.ASCENDING : SortOrder.DESCENDING)
                        .build();
                    var mapa = new java.util.HashMap<String, SortMeta>();
                    mapa.put(orden.getField(), orden);
                    dt.setSortByAsMap(mapa);
                }
                return vista.getString("filtroGlobal", "");
            }
        } catch (RuntimeException vistaCorrupta) {
            return null;
        }
    }

    private DataTable tabla() {
        var ctx = FacesContext.getCurrentInstance();
        if (ctx == null || ctx.getViewRoot() == null) {
            return null;
        }
        return (DataTable) ctx.getViewRoot().findComponent("frmLista:tabla");
    }

    private Long usuarioId() {
        try {
            var sesion = CDI.current().select(SesionUsuario.class).get();
            return sesion.getUsuario() == null ? null : sesion.getUsuario().getId();
        } catch (RuntimeException sinContexto) {
            return null;
        }
    }
}
