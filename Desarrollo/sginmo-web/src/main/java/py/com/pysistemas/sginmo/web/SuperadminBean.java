package py.com.pysistemas.sginmo.web;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import py.com.pysistemas.sginmo.dominio.persona.PersonaJuridica;
import py.com.pysistemas.sginmo.servicio.EmpresaService;

import java.io.Serializable;
import java.util.List;

/**
 * Soporte multiempresa del SUPERADMIN (F6): selector de "operar como" una empresa.
 * Mientras el SUPERADMIN opera como un tenant, TODO el sistema (services + RLS via el
 * interceptor) queda acotado a esa empresa; volver a "Global (-1)" restaura su vision total.
 *
 * Session-scoped: la eleccion acompana la sesion del SUPERADMIN entre pantallas. Solo tiene
 * efecto para un SUPERADMIN real; para el resto de los usuarios el selector no se muestra
 * (rendered=esSuperadmin) y operarComo() del TenantContext lanzaria de todos modos.
 */
@Named
@SessionScoped
public class SuperadminBean implements Serializable {

    @Inject
    private TenantContext tenant;

    @Inject
    private transient EmpresaService empresaService;

    @Inject
    private ContextoEmpresa contexto;

    /** Empresas ofrecidas en el selector; se cachean por sesion. */
    private List<PersonaJuridica> empresas;

    @PostConstruct
    public void iniciar() {
        // no se carga nada hasta que un SUPERADMIN despliegue el selector (getEmpresas()).
    }

    /** true solo para el SUPERADMIN real: gobierna el render del selector en la plantilla. */
    public boolean isEsSuperadmin() {
        return tenant.esSuperadminReal();
    }

    /** Lista de empresas para elegir (el rol EMPRESA vive en -1, visible en cualquier contexto). */
    public List<PersonaJuridica> getEmpresas() {
        if (empresas == null && isEsSuperadmin()) {
            empresas = empresaService.listar(0, 1000, "");
        }
        return empresas == null ? java.util.List.of() : empresas;
    }

    /** Tenant efectivo actual: el override elegido, o -1 (global) si esta en su vision total. */
    public Long getTenantActivo() {
        Long o = tenant.getOverride();
        return o == null ? TenantContext.GLOBAL : o;
    }

    /** Setter del selector: aplica el "operar como" (o vuelve a global con -1). */
    public void setTenantActivo(Long id) {
        tenant.operarComo(id);
    }

    /** Tras cambiar de empresa se refresca el contexto (banner/sucursales del tenant efectivo). */
    public void cambiar() {
        contexto.refrescar();
    }
}
