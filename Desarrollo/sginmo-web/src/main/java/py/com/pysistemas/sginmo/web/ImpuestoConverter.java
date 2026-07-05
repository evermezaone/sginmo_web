package py.com.pysistemas.sginmo.web;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;
import py.com.pysistemas.sginmo.dominio.catalogo.Impuesto;
import py.com.pysistemas.sginmo.servicio.CatalogoService;

/** Converter id<->entidad para combos de Impuesto (converter administrado: permite @Inject). */
@FacesConverter(value = "impuestoConverter", managed = true)
public class ImpuestoConverter implements Converter<Impuesto> {

    @Inject
    private CatalogoService catalogoService;

    @Override
    public Impuesto getAsObject(FacesContext ctx, UIComponent comp, String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return catalogoService.buscarImpuesto(Long.valueOf(valor));
    }

    @Override
    public String getAsString(FacesContext ctx, UIComponent comp, Impuesto impuesto) {
        return impuesto == null || impuesto.getId() == null ? "" : impuesto.getId().toString();
    }
}
