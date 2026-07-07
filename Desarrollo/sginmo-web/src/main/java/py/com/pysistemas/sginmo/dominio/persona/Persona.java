package py.com.pysistemas.sginmo.dominio.persona;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import py.com.one.core.Auditable;

import java.io.Serializable;

/** Persona base (V1): subconjunto que usan empresas (REQ-0009); socios completos en REQ-0012. */
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

    @Column(name = "es_contribuyente", nullable = false)
    private boolean esContribuyente = true;

    @Column(name = "direccion", length = 180)
    private String direccion;

    @Column(name = "telefono", length = 20)
    private String telefono;

    @Column(name = "email", length = 180)
    private String email;

    @Column(name = "estado", length = 10, nullable = false)
    private String estado = "ACTIVO";

    // ── Datos fiscales / ubicacion (columnas de la tabla que faltaban en el ABM) ──
    @Column(name = "tipo_documento_lista", length = 40)
    private String tipoDocumentoLista = "TIPOS_DOCUMENTOS_IDENTIDAD";

    @Column(name = "tipo_documento_codigo", length = 40)
    private String tipoDocumentoCodigo;

    @Column(name = "clasificacion_fiscal", length = 60)
    private String clasificacionFiscal;

    /** FK a ubicacion_geografica (ciudad/barrio). */
    @Column(name = "ubicacion")
    private Long ubicacion;

    @Column(name = "ubicacion_url", length = 250)
    private String ubicacionUrl;

    @Column(name = "observacion")
    private String observacion;

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
    public boolean isEsContribuyente() { return esContribuyente; }
    public void setEsContribuyente(boolean esContribuyente) { this.esContribuyente = esContribuyente; }
    public String getDireccion() { return direccion; }
    public void setDireccion(String direccion) { this.direccion = direccion; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
    public String getTipoDocumentoLista() { return tipoDocumentoLista; }
    public void setTipoDocumentoLista(String v) { this.tipoDocumentoLista = v; }
    public String getTipoDocumentoCodigo() { return tipoDocumentoCodigo; }
    public void setTipoDocumentoCodigo(String v) { this.tipoDocumentoCodigo = v; }
    public String getClasificacionFiscal() { return clasificacionFiscal; }
    public void setClasificacionFiscal(String v) { this.clasificacionFiscal = v; }
    public Long getUbicacion() { return ubicacion; }
    public void setUbicacion(Long v) { this.ubicacion = v; }
    public String getUbicacionUrl() { return ubicacionUrl; }
    public void setUbicacionUrl(String v) { this.ubicacionUrl = v; }
    public String getObservacion() { return observacion; }
    public void setObservacion(String v) { this.observacion = v; }

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
