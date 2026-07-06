package py.com.one.core;

import jakarta.enterprise.inject.spi.CDI;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

import java.time.LocalDateTime;

/**
 * Pobla la auditoria de {@link Auditable} automaticamente.
 * Regla del proyecto (standards/backend-jakarta.md): la auditoria NUNCA se setea a mano
 * — en el legado la poblaban los forms (doc 03) y quedaba inconsistente.
 */
public class AuditoriaListener {

    @PrePersist
    public void alCrear(Auditable entidad) {
        entidad.setUsuarioCreacion(usuarioActual());
        entidad.setFechaCreacion(LocalDateTime.now());
    }

    @PreUpdate
    public void alModificar(Auditable entidad) {
        entidad.setUsuarioModificacion(usuarioActual());
        entidad.setFechaModificacion(LocalDateTime.now());
    }

    private String usuarioActual() {
        try {
            return CDI.current().select(UsuarioActual.class).get().codigoUsuario();
        } catch (RuntimeException sinContexto) {
            // Sin CDI o sin sesion (tests, jobs, ETL): auditar como "sistema".
            return UsuarioActual.SISTEMA;
        }
    }
}
