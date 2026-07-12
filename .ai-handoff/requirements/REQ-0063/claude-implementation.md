# REQ-0063 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0063
- Tipo de cambio: BD (2 tablas + seed + RLS + pantalla) + backend + UI
- Riesgo: medio-alto (seguridad/permisos; escribe permiso_grupo, la autorizacion real)
- Archivos clave:
  - `V44__roles_plantilla.sql`: `rol_plantilla` + `rol_plantilla_permiso` + seed de 7 plantillas base (29 permisos) + pantalla `roles-plantilla`. RLS en rol_plantilla.
  - `servicio/RolPlantillaService.java`: @AislarTenant; plantillas/grupos, diff(plantilla, grupo), aplicar (add-only o reemplazar) escribiendo permiso_grupo; verifica que el grupo sea del tenant; permiso grupos/EDITAR.
  - `web/RolPlantillaBean.java` + `webapp/roles-plantilla.xhtml`: elegir plantilla+grupo, ver diff, aplicar.
  - `WEB-INF/plantilla.xhtml`, `tools/smoke-test-vps.py`: menu + cobertura.
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - V44 en `BEGIN...ROLLBACK`: 7 plantillas + 29 permisos + pantalla.
  - Deploy + Flyway V44 `success=t`; `python tools/smoke-test-vps.py`: 29/29 RENDER OK incl. `roles-plantilla`.
- Cambios de datos: si, V44 (2 tablas + seed de plantillas globales + pantalla).
- Cambios de entorno: no.
- Impacto LLM/tokens: no.
- Decision esperada: revisar seguridad (autorizacion real + no-superadmin).
- Notas para auditor (SEGURIDAD):
  - Autorizacion real: aplicar escribe `permiso_grupo`, que es lo que consulta `SesionUsuario.puede()` en el backend. No es solo UI.
  - No concede SUPERADMIN: aplicar NUNCA toca `usuario.perfil`; solo (pantalla, accion) del grupo.
  - Aislamiento: `aplicar/diff` verifican que el grupo pertenezca al tenant actual (grupo no tiene RLS).
  - No borra sin confirmacion: por defecto add-only; "reemplazar" es explicito con p:confirm.

## Resumen Funcional

Nueva pantalla "Plantillas de roles": 7 perfiles base (administrador/caja/cobrador/ventas/gerencia/auditor/
consulta). Se elige una plantilla y un grupo, se ve el diff (que se agregaria / que se quitaria) y se aplica
(solo agregar, o reemplazar exacto). Los usuarios del grupo obtienen los permisos.

## Resumen Tecnico

RolPlantillaService compara el set de permisos de la plantilla con el del grupo y aplica escribiendo
permiso_grupo (con auditoria). @AislarTenant + verificacion de pertenencia del grupo al tenant.

## Limitaciones Conocidas (transparencia)

- Edicion/creacion de plantillas via UI: las 7 base vienen sembradas; el ABM de edicion es refinamiento.
- Aplicar directo a un USUARIO individual: hoy se aplica al GRUPO (los usuarios heredan por grupo). Refinamiento.
- "Revisar rapidamente los permisos de un usuario": disponible via la pantalla de grupos + este diff; vista consolidada por usuario es refinamiento.

## Archivos Modificados

Ver Manifiesto. V44 nueva.

## Cambios De Datos

V44: `rol_plantilla` + `rol_plantilla_permiso` + 7 plantillas base (29 permisos) + pantalla. Aplicar escribe permiso_grupo.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; V44 rollback OK; deploy + Flyway success; smoke 29/29. Ver test-plan.

## Pruebas Manuales Sugeridas

1. Plantillas de roles -> elegir CAJA + un grupo -> Ver diferencias -> Aplicar (add-only). Verificar que el grupo gano los permisos de caja/comprobantes/arqueo.

## Riesgos Conocidos

- Seguridad/permisos: mitigado (autorizacion real, sin superadmin, aislamiento). Ver "Limitaciones".
