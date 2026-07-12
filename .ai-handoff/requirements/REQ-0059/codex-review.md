# REQ-0059 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-12
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Hallazgos Bloqueantes

- Sin hallazgos bloqueantes en la re-auditoria.

## Evidencia de Re-auditoria

- `ArqueoService` exige `arqueo/EDITAR`, `arqueo/EXPORTAR` y `arqueo/REACTIVAR`; ya no depende de permisos `caja/*` para acciones de arqueo.
- `arqueo.xhtml` renderiza cierre, PDF y reapertura con la misma matriz `arqueo/*`.
- `V47__arqueo_permiso_editar.sql` agrega `arqueo/EDITAR` a la plantilla CAJA y concede el permiso a grupos existentes que ya tenian `arqueo/VER`.
- La reapertura queda separada como `arqueo/REACTIVAR`, coherente con el comentario de V47 y con la criticidad de la accion.

## Pruebas Revisadas

- Revision estatica de `ArqueoService`, `arqueo.xhtml`, `V44__roles_plantilla.sql` y `V47__arqueo_permiso_editar.sql`.
- Build Maven previo: `mvn -q clean package` EXIT 0.
