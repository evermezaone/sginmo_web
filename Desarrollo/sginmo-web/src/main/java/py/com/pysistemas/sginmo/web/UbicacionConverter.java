package py.com.pysistemas.sginmo.web;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;
import py.com.pysistemas.sginmo.dominio.catalogo.UbicacionGeografica;
import py.com.pysistemas.sginmo.servicio.GeografiaService;

/** Converter id<->entidad para el autocomplete de ubicaciones (converter administrado). */
@FacesConverter(value = "ubicacionConverter", managed = true)
public class UbicacionConverter implements Converter<UbicacionGeografica> {

    @Inject
    private GeografiaService geografiaService;

    @Override
    public UbicacionGeografica getAsObject(FacesContext ctx, UIComponent comp, String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return geografiaService.buscarPorId(Long.valueOf(valor));
    }

    @Override
    public String getAsString(FacesContext ctx, UIComponent comp, UbicacionGeografica u) {
        return u == null || u.getId() == null ? "" : u.getId().toString();
    }
}
