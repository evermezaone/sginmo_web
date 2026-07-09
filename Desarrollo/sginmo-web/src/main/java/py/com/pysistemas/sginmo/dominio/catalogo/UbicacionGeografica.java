package py.com.pysistemas.sginmo.dominio.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import py.com.one.core.Auditable;

import java.io.Serializable;

/** Geografia recursiva (V1/V3, INE 2022): pais > departamento > distrito > barrio/localidad. */
@jakarta.persistence.Entity
@Table(name = "ubicacion_geografica")
public class UbicacionGeografica extends Auditable implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ubicacion_geografica")
    private Long id;

    /** Discriminador multiempresa (V26); -1 = catalogo GLOBAL (arbol INE). */
    @Column(name = "tenant")
    private Long tenant;
    public Long getTenant() { return tenant; }
    public void setTenant(Long tenant) { this.tenant = tenant; }

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "padre")
    private UbicacionGeografica padre;

    @Column(name = "nombre", length = 150, nullable = false)
    private String nombre;

    @Column(name = "nivel", nullable = false)
    private Long nivel;              // FK bigint a entidad (lista NIVELES_UBICACION)

    @Column(name = "codigo_oficial", length = 10)
    private String codigoOficial;

    @Column(name = "estado", length = 10, nullable = false)
    private String estado = "ACTIVO";

    /** Nombre con contexto para combos/autocomplete: "San Roque (Barrio, Asunción)". */
    public String getEtiqueta() {
        return nombre + (padre != null ? " — " + padre.getNombre() : "");
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public UbicacionGeografica getPadre() { return padre; }
    public void setPadre(UbicacionGeografica padre) { this.padre = padre; }
    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }
    public Long getNivel() { return nivel; }
    public void setNivel(Long nivel) { this.nivel = nivel; }
    public String getCodigoOficial() { return codigoOficial; }
    public void setCodigoOficial(String codigoOficial) { this.codigoOficial = codigoOficial; }
    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof UbicacionGeografica)) return false;
        UbicacionGeografica otro = (UbicacionGeografica) o;
        return id != null && id.equals(otro.id);
    }

    @Override
    public int hashCode() { return getClass().hashCode(); }
}
