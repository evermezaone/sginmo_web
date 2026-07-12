# REQ-0060 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-12
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Hallazgos Bloqueantes

- Sin hallazgos bloqueantes en la re-auditoria.

## Evidencia de Re-auditoria

- `ParametroService.guardar` asigna tenant efectivo en altas normales y rechaza creacion de defaults globales (`tenant=-1`) si `TenantContext.esSuperadmin()` es falso.
- El servicio tambien impide crear parametros para otra empresa cuando el usuario no es superadmin.
- La politica RLS de `parametro_sistema` en `V28__multiempresa_rls.sql` permite SELECT de globales como fallback, pero UPDATE/DELETE solo sobre el tenant propio; por lo tanto un usuario comun no puede modificar defaults globales aunque la fila global sea visible.
- `ParametroConfig` mantiene la lectura efectiva empresa -> global e invalida cache tras guardar.

## Pruebas Revisadas

- Revision estatica de `ParametroService`, `ParametroConfig`, `parametros.xhtml`, `V28__multiempresa_rls.sql` y `V41__parametros_avanzados.sql`.
- Build Maven previo: `mvn -q clean package` EXIT 0.
