package py.com.pysistemas.sginmo.dominio.catalogo;

import java.io.Serializable;
import java.util.Objects;

/** Clave compuesta de parametro_sistema (V26): (tenant, clave). -1 = defaults globales. */
public class ParametroSistemaId implements Serializable {

    private Long tenant;
    private String clave;

    public ParametroSistemaId() { }

    public ParametroSistemaId(Long tenant, String clave) {
        this.tenant = tenant;
        this.clave = clave;
    }

    public Long getTenant() { return tenant; }
    public void setTenant(Long tenant) { this.tenant = tenant; }
    public String getClave() { return clave; }
    public void setClave(String clave) { this.clave = clave; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ParametroSistemaId otra)) return false;
        return Objects.equals(tenant, otra.tenant) && Objects.equals(clave, otra.clave);
    }

    @Override
    public int hashCode() { return Objects.hash(tenant, clave); }
}
