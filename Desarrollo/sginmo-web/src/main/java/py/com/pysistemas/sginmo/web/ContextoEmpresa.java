package py.com.pysistemas.sginmo.web;

import jakarta.enterprise.context.SessionScoped;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import py.com.one.security.servicio.PreferenciaService;
import py.com.one.security.web.SesionUsuario;
import py.com.pysistemas.sginmo.dominio.persona.PersonaJuridica;
import py.com.pysistemas.sginmo.dominio.persona.Sucursal;
import py.com.pysistemas.sginmo.servicio.EmpresaService;

import java.io.Serializable;
import java.util.List;

/**
 * Contexto empresa/sucursal en sesion (REQ-0009, diferido de REQ-0005): la empresa
 * viene del usuario logueado (usuario.empresa) y la sucursal activa es seleccionable
 * en la barra superior; la ultima eleccion se recuerda por usuario (preferencia_usuario).
 */
@Named
@SessionScoped
public class ContextoEmpresa implements Serializable {

    private static final String PANTALLA = "*";
    private static final String CLAVE = "sucursal_activa";

    @Inject
    private SesionUsuario sesion;

    @Inject
    private transient EmpresaService empresaService;

    @Inject
    private transient PreferenciaService preferencias;

    private boolean cargado;
    private PersonaJuridica empresa;
    private List<Sucursal> sucursales = java.util.List.of();
    private Long sucursalActiva;

    private void cargar() {
        if (cargado || !sesion.isLogueado()) {
            return;
        }
        empresa = empresaService.buscar(sesion.getUsuario().getTenant());
        if (empresa != null) {
            sucursales = empresaService.sucursalesActivasDe(empresa.getId());
            String guardada = preferencias.leer(sesion.getUsuario().getId(), PANTALLA, CLAVE);
            if (guardada != null) {
                try {
                    Long id = Long.valueOf(guardada);
                    if (sucursales.stream().anyMatch(s -> s.getId().equals(id))) {
                        sucursalActiva = id;
                    }
                } catch (NumberFormatException ignorada) { }
            }
            if (sucursalActiva == null && !sucursales.isEmpty()) {
                sucursalActiva = sucursales.stream().filter(Sucursal::isPorDefecto)
                    .findFirst().orElse(sucursales.get(0)).getId();
            }
        }
        cargado = true;
    }

    /** La eleccion de sucursal se recuerda por usuario. */
    public void guardarSeleccion() {
        if (sucursalActiva != null && sesion.isLogueado()) {
            preferencias.guardar(sesion.getUsuario().getId(), PANTALLA, CLAVE, sucursalActiva.toString());
        }
    }

    /** Refresca el contexto (tras editar empresa/sucursales). */
    public void refrescar() {
        cargado = false;
        cargar();
    }

    public String getNombreEmpresa() {
        cargar();
        // V26: el nombre de fantasia vive en persona_empresa POR TENANT; la barra usa la razon
        // social (identidad). El fantasia por tenant se expondra en F6 (gestion de empresa).
        return empresa == null ? "" : empresa.getRazonSocial();
    }

    public boolean isTieneSucursales() {
        cargar();
        return !sucursales.isEmpty();
    }

    public List<Sucursal> getSucursales() {
        cargar();
        return sucursales;
    }

    public Long getSucursalActiva() {
        cargar();
        return sucursalActiva;
    }

    public void setSucursalActiva(Long sucursalActiva) {
        this.sucursalActiva = sucursalActiva;
    }

    /** Para los modulos futuros (documentos/cobros): sucursal del contexto o null. */
    public Sucursal sucursal() {
        cargar();
        return sucursales.stream().filter(s -> s.getId().equals(sucursalActiva)).findFirst().orElse(null);
    }

    public PersonaJuridica getEmpresa() {
        cargar();
        return empresa;
    }
}
