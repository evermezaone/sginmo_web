package py.com.pysistemas.sginmo.dominio.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import py.com.one.core.Auditable;

import java.io.Serializable;

/**
 * Parametros de configuracion. V26: PK compuesta (tenant, clave); -1 = defaults globales.
 * La misma clave puede existir por tenant (override de la empresa sobre el default -1).
 */
@jakarta.persistence.Entity
@Table(name = "parametro_sistema")
@IdClass(ParametroSistemaId.class)
public class ParametroSistema extends Auditable implements Serializable {

    /** Discriminador multiempresa (V26); parte de la PK. -1 = default global. */
    @Id
    @Column(name = "tenant")
    private Long tenant;

    @Id
    @Column(name = "clave", length = 60)
    private String clave;

    @Column(name = "valor", length = 120, nullable = false)
    private String valor;

    @Column(name = "descripcion", length = 180, nullable = false)
    private String descripcion;

    // ── REQ-0060: metadatos de parametrizacion avanzada ──
    @Column(name = "tipo", length = 12)
    private String tipo = "STRING";     // STRING|ENTERO|DECIMAL|BOOLEAN|FECHA
    @Column(name = "grupo", length = 40)
    private String grupo;
    @Column(name = "valor_defecto", length = 120)
    private String valorDefecto;

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }
    public String getGrupo() { return grupo; }
    public void setGrupo(String grupo) { this.grupo = grupo; }
    public String getValorDefecto() { return valorDefecto; }
    public void setValorDefecto(String v) { this.valorDefecto = v; }

    /** true si el valor es sensible (clave/contrasena): se enmascara en la grilla. */
    public boolean isSensible() {
        return clave != null && (clave.contains("CLAVE") || clave.contains("PASS"));
    }

    public Long getTenant() { return tenant; }
    public void setTenant(Long tenant) { this.tenant = tenant; }
    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }
    public String getValor() { return valor; }
    public void setValor(String valor) { this.valor = valor; }
    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParametroSistema)) return false;
        ParametroSistema otro = (ParametroSistema) o;
        return clave != null && clave.equals(otro.clave)
                && java.util.Objects.equals(tenant, otro.tenant);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
