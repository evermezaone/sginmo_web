package py.com.pysistemas.sginmo.web;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;
import py.com.pysistemas.sginmo.dominio.activo.Activo;
import py.com.pysistemas.sginmo.servicio.ActivoService;

/** Converter id<->Activo para el autocomplete de contenedor (padre). */
@FacesConverter(value = "activoConverter", managed = true)
public class ActivoConverter implements Converter<Activo> {

    @Inject
    private ActivoService activoService;

    @Override
    public Activo getAsObject(FacesContext ctx, UIComponent comp, String valor) {
        return valor == null || valor.isBlank() ? null : activoService.buscar(Long.valueOf(valor));
    }

    @Override
    public String getAsString(FacesContext ctx, UIComponent comp, Activo a) {
        return a == null || a.getId() == null ? "" : a.getId().toString();
    }
}
