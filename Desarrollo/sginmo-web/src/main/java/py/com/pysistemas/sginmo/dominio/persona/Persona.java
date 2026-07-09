package py.com.pysistemas.sginmo.dominio.persona;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import py.com.one.core.Auditable;

import java.io.Serializable;

/**
 * Persona base — SOLO identidad GLOBAL (V26, doc 14 §2). Se comparte entre empresas
 * por numero_documento; los datos comerciales/contextuales (direccion, telefono, email,
 * es_contribuyente, etc.) viven en persona_empresa POR TENANT.
 */
@jakarta.persistence.Entity
@Table(name = "persona")
public class Persona extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "persona")
    private Long id;

    @Column(name = "tipo_personeria", length = 10, nullable = false)
    private String tipoPersoneria = "JURIDICA";   // FISICA | JURIDICA

    /** Nombre visible: nombre completo o razon social. */
    @Column(name = "nombre", length = 180, nullable = false)
    private String nombre;

    @Column(name = "numero_documento", length = 20, nullable = false, unique = true)
    private String numeroDocumento;

    @Column(name = "digito_verificador", length = 1)
    private String digitoVerificador;

    /** Tipo de documento por id (V26): FK bigint a entidad (lista TIPOS_DOCUMENTOS_IDENTIDAD). */
    @Column(name = "tipo_documento")
    private Long tipoDocumento;

    @Column(name = "estado", length = 10, nullable = false)
    private String estado = "ACTIVO";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTipoPersoneria() { return tipoPersoneria; }
    public void setTipoPersoneria(String tipoPersoneria) { this.tipoPersoneria = tipoPersoneria; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public String getNumeroDocumento() { return numeroDocumento; }
    public void setNumeroDocumento(String numeroDocumento) { this.numeroDocumento = numeroDocumento; }
    public String getDigitoVerificador() { return digitoVerificador; }
    public void setDigitoVerificador(String digitoVerificador) { this.digitoVerificador = digitoVerificador; }
    public Long getTipoDocumento() { return tipoDocumento; }
    public void setTipoDocumento(Long v) { this.tipoDocumento = v; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Persona)) return false;
        Persona otro = (Persona) o;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
