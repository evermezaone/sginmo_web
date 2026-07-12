# REQ-0063 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-12
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos Bloqueantes

- `rol_plantilla_permiso` no tiene RLS y `RolPlantillaService.permisosPlantilla` carga permisos solo por `plantillaId`, sin validar que la plantilla sea global o del tenant actual. Con un id conocido/manipulado, se podria aplicar una plantilla de otro tenant a un grupo propio.
- La plantilla CAJA no concede permisos consistentes con el arqueo implementado en REQ-0059 (`caja/EDITAR`, `caja/EXPORTAR`, `caja/REACTIVAR` o equivalentes `arqueo/*`), dejando incompleto un perfil operativo clave.

## Solucion Esperada

- Validar siempre la cabecera `rol_plantilla` antes de leer/aplicar sus permisos: `tenant=-1`, tenant actual o SUPERADMIN.
- Considerar RLS o join obligatorio contra `rol_plantilla` para el detalle.
- Corregir la plantilla CAJA junto con la matriz final de permisos de arqueo.

## Pruebas Revisadas

- Revision estatica de `RolPlantillaService`, `roles-plantilla.xhtml` y `V44__roles_plantilla.sql`.
- Build Maven previo: `mvn -q clean package` EXIT 0.
