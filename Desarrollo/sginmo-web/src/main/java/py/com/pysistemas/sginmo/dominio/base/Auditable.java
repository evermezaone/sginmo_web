package py.com.pysistemas.sginmo.dominio.base;

import jakarta.persistence.Column;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.MappedSuperclass;

import java.time.LocalDateTime;

/**
 * Auditoria comun a todas las tablas del legado (doc 02):
 * USUARIO_CREACION / FECHA_CREACION (NOT NULL) y USUARIO_MODIFICACION / FECHA_MODIFICACION.
 * Poblada por {@link AuditoriaListener}; prohibido setearla desde beans o servicios.
 */
@MappedSuperclass
@EntityListeners(AuditoriaListener.class)
public abstract class Auditable {

    @Column(name = "usuario_creacion", length = 20, nullable = false, updatable = false)
    private String usuarioCreacion;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "usuario_modificacion", length = 20)
    private String usuarioModificacion;

    @Column(name = "fecha_modificacion")
    private LocalDateTime fechaModificacion;

    public String getUsuarioCreacion() { return usuarioCreacion; }
    void setUsuarioCreacion(String usuarioCreacion) { this.usuarioCreacion = usuarioCreacion; }

    public LocalDateTime getFechaCreacion() { return fechaCreacion; }
    void setFechaCreacion(LocalDateTime fechaCreacion) { this.fechaCreacion = fechaCreacion; }

    public String getUsuarioModificacion() { return usuarioModificacion; }
    void setUsuarioModificacion(String usuarioModificacion) { this.usuarioModificacion = usuarioModificacion; }

    public LocalDateTime getFechaModificacion() { return fechaModificacion; }
    void setFechaModificacion(LocalDateTime fechaModificacion) { this.fechaModificacion = fechaModificacion; }
}
