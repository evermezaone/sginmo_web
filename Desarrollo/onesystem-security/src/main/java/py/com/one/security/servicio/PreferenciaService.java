package py.com.one.security.servicio;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.transaction.Transactional;
import py.com.one.security.dominio.PreferenciaUsuario;

/** Preferencias de pantalla por usuario (REQ-0004): upsert por (usuario, pantalla, clave). */
@ApplicationScoped
public class PreferenciaService {

    @PersistenceContext
    private EntityManager em;

    public String leer(Long usuario, String pantalla, String clave) {
        return em.createQuery(
                "SELECT p.valor FROM PreferenciaUsuario p WHERE p.usuario = :u AND p.pantalla = :p AND p.clave = :c",
                String.class)
            .setParameter("u", usuario).setParameter("p", pantalla).setParameter("c", clave)
            .getResultStream().findFirst().orElse(null);
    }

    @Transactional
    public void guardar(Long usuario, String pantalla, String clave, String valor) {
        PreferenciaUsuario existente = em.createQuery(
                "SELECT p FROM PreferenciaUsuario p WHERE p.usuario = :u AND p.pantalla = :p AND p.clave = :c",
                PreferenciaUsuario.class)
            .setParameter("u", usuario).setParameter("p", pantalla).setParameter("c", clave)
            .getResultStream().findFirst().orElse(null);
        if (existente != null) {
            existente.setValor(valor);
            return;
        }
        var nueva = new PreferenciaUsuario();
        nueva.setUsuario(usuario);
        nueva.setPantalla(pantalla);
        nueva.setClave(clave);
        nueva.setValor(valor);
        em.persist(nueva);
    }

    @Transactional
    public void eliminar(Long usuario, String pantalla, String clave) {
        em.createQuery(
                "DELETE FROM PreferenciaUsuario p WHERE p.usuario = :u AND p.pantalla = :p AND p.clave = :c")
            .setParameter("u", usuario).setParameter("p", pantalla).setParameter("c", clave)
            .executeUpdate();
    }
}
