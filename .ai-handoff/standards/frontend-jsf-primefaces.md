# Estandar Frontend — JSF / PrimeFaces (SGInmo Web)

## Estructura

- Vistas Facelets en `src/main/webapp/`, organizadas por módulo (`mantenimiento/`, `entidades/`, `operaciones/`, `cobros/`, `reportes/`...).
- Plantilla única `WEB-INF/templates/layout.xhtml` con menú por rol; toda página usa `ui:composition`.
- Componentes reutilizables (`ui:include`/composite) para los patrones del legado: ABM (ex-FrmMaestro) y buscador modal (ex-FrmBuscadorGenerico). No copiar/pegar el mismo dialog en cada página.

## Beans

- Un backing bean por vista, `@ViewScoped`, nombre `XxxBean`. Sin lógica de negocio (ver backend-jakarta.md).
- Listas grandes SIEMPRE con `LazyDataModel` (paginación server-side); nunca cargar tablas completas a memoria.
- Estado de sesión (usuario, empresa, sucursal activa) solo en `SesionBean` `@SessionScoped`.

## Patrones PrimeFaces

- Grillas: `p:dataTable` con `lazy=true`, filtros y `rowsPerPage`. Selección → `p:ajax`.
- Buscadores: `p:dialog` modal reutilizable o `p:autoComplete` para socios/propiedades/operaciones.
- Mensajes: `p:growl` global + `FacesMessage`; nunca `alert()` JS para flujo de negocio.
- Confirmaciones destructivas (anular, eliminar, rescindir): `p:confirmDialog` obligatorio.
- Formato Gs.: `f:convertNumber` con locale es-PY / patrón `#,##0`; fechas `dd/MM/yyyy`.

## Reglas de UI heredadas a conservar

- Cuotas vencidas resaltadas (rojo) en la pantalla de cobros (RN-COBR-005).
- Estado de propiedad LIBRE/OCUPADA con color (RN-PROP-003).
- Autoformato de número de comprobante "1-1-123" → "001-001-0000123".
- Campos numéricos con validación de entrada (equivalente de camposNumericos) — pero la validación real está en backend.

## Seguridad en UI

- Menú y botones filtrados por rol (`rendered=...`), COMPLEMENTARIO al `@RolesAllowed` del backend.
- Ninguna página fuera del control de acceso: `web.xml`/filtro cubre `/*` salvo login y recursos.

## Verificación

- La vista debe compilar y renderizar sin errores EL en el log de WildFly.
- Toda ruta/`outcome` de navegación referenciada debe existir como `.xhtml`.
