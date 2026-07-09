package py.com.pysistemas.sginmo.dominio.catalogo;

import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import py.com.one.core.Auditable;

/**
 * Listas abiertas configurables (tabla generica `entidad`). Multiempresa (V26):
 * PK numerica autonumerica + indice unico (lista, codigo, tenant). El mismo codigo
 * puede existir por tenant; -1 = opcion GLOBAL (bloqueada, solo SUPERADMIN).
 * Los campos que referencian una opcion guardan el id numerico (FK a entidad).
 * Filtro estandar de combos: WHERE lista = :lista AND tenant IN (-1, :tenant).
 */
@jakarta.persistence.Entity
@Table(name = "entidad")
public class Entidad extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "entidad")
    private Long id;

    /** Nombre de la lista (ex columna 'entidad'): TIPOS_CONTRATOS, ESTADOS_CIVILES, ... */
    @Column(name = "lista", length = 40, nullable = false)
    private String lista;

    @Column(name = "codigo", length = 40, nullable = false)
    private String codigo;

    /** Discriminador multiempresa; -1 = opcion GLOBAL bloqueada. */
    @Column(name = "tenant", nullable = false)
    private Long tenant;

    @Column(name = "descripcion", length = 180, nullable = false)
    private String descripcion;

    @Column(name = "valor_texto", length = 250)
    private String valorTexto;

    @Column(name = "estado", length = 10, nullable = false)
    private String estado = "ACTIVO";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getLista() { return lista; }
    public void setLista(String lista) { this.lista = lista; }

    public String getCodigo() { return codigo; }
    public void setCodigo(String codigo) { this.codigo = codigo; }

    public Long getTenant() { return tenant; }
    public void setTenant(Long tenant) { this.tenant = tenant; }

    public String getDescripcion() { return descripcion; }
    public void setDescripcion(String descripcion) { this.descripcion = descripcion; }

    public String getValorTexto() { return valorTexto; }
    public void setValorTexto(String valorTexto) { this.valorTexto = valorTexto; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }
}
