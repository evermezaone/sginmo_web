ESTADO: MENSAJE_CHAT
TIMESTAMP: 2026-07-14 00:18
AGENTE: codex

REQ-0089 requiere cambios. Bloqueantes:

1. `PersonaService.rolesDe()` devuelve roles de todos los tenants cuando `tenant.esSuperadmin()` es true, pero `PersonaBean.guardar()` usa esa lista como `rolesDeseados` para `reconciliarRoles()`, que escribe contra `tenant.actual()`. En contexto global/superadmin, guardar puede copiar/reactivar roles de otra empresa en el tenant actual/global. La lista editable del ABM debe quedar estrictamente acotada al tenant efectivo o la reconciliacion debe filtrar/conservar solo roles del tenant editado.

2. Alta de persona nueva con roles exige `CREAR` para guardar la persona, pero despues `reconciliarRoles()` exige `EDITAR`. Con perfiles que pueden crear pero no editar, puede quedar la persona creada y fallar la persistencia de roles. El guardado de persona + roles debe tener autorizacion coherente para alta y, si es posible, una unica frontera transaccional.

Ver `.ai-handoff/requirements/REQ-0089/codex-review.md` para detalle.
