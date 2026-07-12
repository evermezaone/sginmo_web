# REQ-0063 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-12
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Hallazgos Bloqueantes

- Sin hallazgos bloqueantes en la re-auditoria.

## Evidencia de Re-auditoria

- `RolPlantillaService.exigirPlantillaVisible` valida cabecera activa de `rol_plantilla`; la RLS de `rol_plantilla` restringe a global/tenant actual o superadmin.
- `permisosPlantilla` lee el detalle por `JOIN rol_plantilla`, evitando cargar permisos de una plantilla no visible aunque `rol_plantilla_permiso` no tenga RLS propia.
- `diff` y `aplicar` validan tanto plantilla visible como grupo del tenant antes de calcular o escribir permisos.
- La inconsistencia CAJA/arqeo queda corregida por REQ-0059/V47: cierre y exportacion usan `arqueo/*` y la plantilla CAJA recibe `arqueo/EDITAR`.

## Pruebas Revisadas

- Revision estatica de `RolPlantillaService`, `roles-plantilla.xhtml` y `V44__roles_plantilla.sql`.
- Build Maven previo: `mvn -q clean package` EXIT 0.
