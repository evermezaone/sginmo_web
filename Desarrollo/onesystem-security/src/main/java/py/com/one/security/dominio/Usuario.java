package py.com.one.security.dominio;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import py.com.one.core.Auditable;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * Usuario del sistema (REQ-0004): login con bcrypt, perfil y bloqueo por intentos.
 * Serializable porque vive en la sesion web.
 */
@jakarta.persistence.Entity
@Table(name = "usuario")
public class Usuario extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "usuario")
    private Long id;

    @Column(name = "codigo_usuario", length = 20, nullable = false, unique = true)
    private String codigoUsuario;

    /** Hash bcrypt; NUNCA se muestra ni se registra en logs. */
    @Column(name = "password_hash", length = 100, nullable = false)
    private String passwordHash;

    @Column(name = "perfil", length = 20, nullable = false)
    private String perfil;                   // ADMINISTRADOR | USUARIO

    @Column(name = "estado", length = 10, nullable = false)
    private String estado = "ACTIVO";

    @Column(name = "empresa", nullable = false)
    private Long empresa;

    @Column(name = "persona")
    private Long persona;

    @Column(name = "intentos_fallidos", nullable = false)
    private Integer intentosFallidos = 0;

    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;

    /** true: al proximo ingreso se exige cambiar la contrasena (alta y reseteo). */
    @Column(name = "debe_cambiar_password", nullable = false)
    private Boolean debeCambiarPassword = Boolean.TRUE;

    /** Base para validacion de dos pasos y alertas de acceso fallido (V10). */
    @Column(name = "email", length = 180)
    private String email;

    @Column(name = "telefono", length = 30)
    private String telefono;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCodigoUsuario() { return codigoUsuario; }
    public void setCodigoUsuario(String codigoUsuario) { this.codigoUsuario = codigoUsuario; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getPerfil() { return perfil; }
    public void setPerfil(String perfil) { this.perfil = perfil; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public Long getEmpresa() { return empresa; }
    public void setEmpresa(Long empresa) { this.empresa = empresa; }

    public Long getPersona() { return persona; }
    public void setPersona(Long persona) { this.persona = persona; }

    public Integer getIntentosFallidos() { return intentosFallidos; }
    public void setIntentosFallidos(Integer intentosFallidos) { this.intentosFallidos = intentosFallidos; }

    public LocalDateTime getBloqueadoHasta() { return bloqueadoHasta; }
    public void setBloqueadoHasta(LocalDateTime bloqueadoHasta) { this.bloqueadoHasta = bloqueadoHasta; }

    public Boolean getDebeCambiarPassword() { return debeCambiarPassword; }
    public void setDebeCambiarPassword(Boolean debeCambiarPassword) { this.debeCambiarPassword = debeCambiarPassword; }
    public boolean isDebeCambiar() { return Boolean.TRUE.equals(debeCambiarPassword); }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }

    /** Igualdad por id (regla del estandar para entidades usadas fuera del contexto de persistencia). */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Usuario otro)) return false;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
