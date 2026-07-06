package py.com.pysistemas.sginmo.web;

import jakarta.faces.component.UIComponent;
import jakarta.faces.context.FacesContext;
import jakarta.faces.convert.Converter;
import jakarta.faces.convert.FacesConverter;
import jakarta.inject.Inject;
import py.com.pysistemas.sginmo.dominio.persona.Persona;
import py.com.pysistemas.sginmo.servicio.PersonaService;

/** Converter id<->Persona para combos de cliente/vendedor/propietario. */
@FacesConverter(value = "personaConverter", managed = true)
public class PersonaConverter implements Converter<Persona> {

    @Inject
    private PersonaService personaService;

    @Override
    public Persona getAsObject(FacesContext ctx, UIComponent comp, String valor) {
        if (valor == null || valor.isBlank()) {
            return null;
        }
        return personaService.buscar(Long.valueOf(valor));
    }

    @Override
    public String getAsString(FacesContext ctx, UIComponent comp, Persona p) {
        return p == null || p.getId() == null ? "" : p.getId().toString();
    }
}
