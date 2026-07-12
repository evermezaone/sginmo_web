# REQ-0059 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-12
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos Bloqueantes

- La pantalla y la plantilla de roles introducen permisos `arqueo/VER` y `arqueo/EXPORTAR`, pero los botones y `ArqueoService` exigen permisos de `caja` (`caja/EDITAR`, `caja/EXPORTAR`, `caja/REACTIVAR`). El perfil CAJA sembrado en `V44` no concede esas acciones, por lo que un usuario con permisos de arqueo puede ver el modulo pero no cerrar, exportar o reabrir segun la matriz nueva.

## Solucion Esperada

- Unificar la matriz: usar `arqueo/EDITAR`, `arqueo/EXPORTAR`, `arqueo/REACTIVAR` en UI y servicio, o sembrar/justificar explicitamente los permisos `caja/*` requeridos.
- Actualizar la plantilla CAJA para que el flujo completo de arqueo quede operable y auditado.

## Pruebas Revisadas

- Revision estatica de `ArqueoService`, `arqueo.xhtml`, `V40__caja_arqueo.sql` y `V44__roles_plantilla.sql`.
- Build Maven previo: `mvn -q clean package` EXIT 0.
