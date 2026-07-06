package py.com.pysistemas.sginmo.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import py.com.one.security.servicio.OpcionPantalla;
import py.com.one.security.servicio.ProveedorPantallas;
import py.com.one.security.servicio.ProveedorParametros;

import java.util.List;

/**
 * Integracion de SGInmo con ONEsystem-security: expone las pantallas autorizables
 * (tabla generica `entidad`, lista PANTALLAS) y los parametros configurables
 * (tabla parametro_sistema: LOGIN_*, SMTP_*, ALERTA_LOGIN_FALLIDO).
 */
@ApplicationScoped
public class IntegracionSeguridad implements ProveedorPantallas, ProveedorParametros {

    @PersistenceContext(unitName = "sginmoPU")
    private EntityManager em;

    @Inject
    private CatalogoService catalogoService;

    @Override
    public List<OpcionPantalla> pantallas() {
        return catalogoService.opciones("PANTALLAS").stream()
            .map(e -> new OpcionPantalla(e.getCodigo(), e.getDescripcion()))
            .toList();
    }

    @Override
    public String valor(String clave) {
        var filas = em.createNativeQuery("SELECT valor FROM parametro_sistema WHERE clave = :clave")
            .setParameter("clave", clave)
            .getResultList();
        return filas.isEmpty() || filas.get(0) == null ? null : filas.get(0).toString();
    }
}
