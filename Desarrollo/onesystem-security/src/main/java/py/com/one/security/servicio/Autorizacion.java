package py.com.one.security.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.spi.CDI;
import py.com.one.core.NegocioException;
import py.com.one.security.web.SesionUsuario;

/**
 * Enforcement de permisos EN LA CAPA DE SERVICIO (obs 203 de Codex): los checks de la UI
 * (rendered/beans) quedan como complemento; la ultima palabra la tiene el servicio
 * transaccional llamando a exigir() antes de cada escritura.
 *
 * Fuera de una sesion web (jobs, ETL, tests de integracion) no hay SesionUsuario:
 * en ese caso se permite la operacion (proceso de sistema), igual que la auditoria
 * usa el fallback "sistema". Toda operacion disparada por un usuario web SI pasa
 * por la sesion y se valida.
 */
@ApplicationScoped
public class Autorizacion {

    /** Lanza NegocioException si el usuario de la sesion actual no puede pantalla:accion. */
    public void exigir(String pantalla, String accion) {
        SesionUsuario sesion;
        try {
            sesion = CDI.current().select(SesionUsuario.class).get();
            // fuerza la resolucion del proxy de sesion; sin contexto activo lanza
            sesion.isLogueado();
        } catch (RuntimeException sinContextoWeb) {
            return;   // proceso de sistema (job/ETL/test): sin sesion no se bloquea
        }
        if (!sesion.puede(pantalla, accion)) {
            throw new NegocioException("No tiene permiso para la acción " + accion
                    + " en la pantalla '" + pantalla + "'");
        }
    }
}
