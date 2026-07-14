# REQ-0089 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-14
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Reauditoria Ronda 2

- La observacion de mezcla de tenants queda cerrada: `PersonaService.rolesDe()` ahora filtra siempre por `r.tenant = tenant.actual()`, tambien para SUPERADMIN. La lista editable y la reconciliacion trabajan sobre el mismo tenant efectivo.
- La observacion de alta parcial queda cerrada: `PersonaBean.guardar()` llama a `guardarFisicaConRoles()` / `guardarJuridicaConRoles()`, y el service guarda persona + roles dentro de una unica transaccion con autorizacion coherente (`CREAR` en alta, `EDITAR` en edicion).
- La reconciliacion sigue haciendo baja logica (`INACTIVO`) de roles desmarcados, reactiva roles existentes inactivos y crea los faltantes solo para `tenant.actual()`.

## Hallazgos

### Bloqueantes

- Ninguno.

### No Bloqueantes

- Queda una doble cabecera de comentario JavaDoc antes de los metodos `guardar*ConRoles`; no afecta compilacion ni comportamiento.

## Riesgos

- Bajo. La correccion toca persistencia de roles, pero ahora la lista y la escritura usan el mismo tenant efectivo y la frontera transaccional del service cubre persona + roles.

## Pruebas Revisadas

- [x] Revision estatica de `PersonaService`.
- [x] Revision estatica de `PersonaBean`.
- [x] Revision estatica de `personas.xhtml`.
- [x] Revision de `TenantContext`.
- [x] `mvn -q -pl sginmo-web -am clean package` EXIT 0.
- [x] Rebusqueda de referencias obsoletas (`guardarRolesPendientes`, llamada separada a `reconciliarRoles` desde el bean).

## Pruebas Faltantes

- [ ] Prueba manual: editar persona en tenant normal, agregar/quitar roles y guardar.
- [ ] Prueba manual: alta de persona con usuario que tenga `CREAR` sin `EDITAR`.
- [ ] Prueba manual/controlada: superadmin global o soporte, persona con roles en mas de un tenant, guardar sin cambios y verificar que no se copian roles a otro tenant.
