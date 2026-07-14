# REQ-0089 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-13
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

1. `PersonaService.rolesDe()` no respeta el tenant efectivo cuando `tenant.esSuperadmin()` es true, pero `PersonaBean.guardar()` usa esa lista completa como `rolesDeseados` para `reconciliarRoles()`. En contexto global/superadmin, guardar una persona puede tomar roles activos de otros tenants visibles en la grilla del dialogo y crear/reactivar esos mismos roles en el tenant actual/global, aunque el usuario no los haya agregado para ese tenant. Esto contradice el criterio "no toca roles de otra empresa". La lista editable del ABM debe ser estrictamente del tenant efectivo, o la reconciliacion debe recibir y conservar solo roles del tenant que se esta editando.

2. Alta de persona nueva con roles no es consistente para perfiles con permiso `CREAR` pero sin `EDITAR`: `guardarFisica/guardarJuridica()` autoriza `CREAR`, pero luego `reconciliarRoles()` exige `EDITAR`. Como son llamadas separadas desde el bean, puede quedar la persona creada y fallar la persistencia de roles, incumpliendo el criterio "Alta de persona nueva con roles sigue funcionando". La operacion de guardar persona + roles debe tener una autorizacion coherente para alta y, preferentemente, una unica frontera transaccional.

### No Bloqueantes

- El modelo diferido del bean corrige la causa raiz para el caso simple de edicion en tenant normal: agregar/quitar roles modifica memoria y al guardar inserta/reactiva o inactiva.

## Riesgos

- Persistencia parcial en alta con roles si falla la reconciliacion despues de guardar la persona.
- Contaminacion de roles entre contextos de tenant al editar como superadmin/global.

## Pruebas Revisadas

- [x] Revision estatica de `PersonaService`.
- [x] Revision estatica de `PersonaBean`.
- [x] Revision estatica de `personas.xhtml`.
- [x] Revision de `TenantContext`.
- [x] `mvn -q -pl sginmo-web -am clean package` EXIT 0.

## Pruebas Faltantes

- [ ] Prueba manual: editar persona en tenant normal, agregar/quitar roles y guardar.
- [ ] Prueba manual: alta de persona con usuario que tenga `CREAR` sin `EDITAR`.
- [ ] Prueba manual/controlada: superadmin global o soporte, persona con roles en mas de un tenant, guardar sin cambios y verificar que no se copian roles a otro tenant.
