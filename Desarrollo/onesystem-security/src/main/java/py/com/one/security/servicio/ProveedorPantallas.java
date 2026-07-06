package py.com.one.security.servicio;

import java.util.List;

/**
 * Punto de integracion del modulo: cada proyecto anfitrion expone sus pantallas
 * autorizables (en SGInmo salen de la tabla generica `entidad`, lista PANTALLAS).
 * Debe existir exactamente UN bean CDI que lo implemente.
 */
public interface ProveedorPantallas {

    List<OpcionPantalla> pantallas();
}
