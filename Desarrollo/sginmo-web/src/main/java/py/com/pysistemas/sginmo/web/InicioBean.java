package py.com.pysistemas.sginmo.web;

import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Named;

/**
 * Bean de la página de inicio del esqueleto (REQ-0001).
 * Verifica que CDI + JSF + PrimeFaces están operativos en WildFly 40.
 */
@Named
@RequestScoped
public class InicioBean {

    public String getSistema() {
        return "SGInmo Web";
    }

    public String getVersion() {
        return "0.1.0-SNAPSHOT";
    }

    public String getStack() {
        return "WildFly 40 · Jakarta EE 11 · Faces 4.1 · PrimeFaces 15 · Java "
                + System.getProperty("java.version");
    }

    public String getHora() {
        return java.time.LocalTime.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
    }
}
