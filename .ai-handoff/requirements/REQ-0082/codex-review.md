# REQ-0082 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-13
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Hallazgos

### Bloqueantes

- Ninguno.

### No Bloqueantes

- Ninguno.

## Validacion

- Existe `webapp/portal/index.xhtml`, resolviendo el welcome-file de la carpeta `/portal/`.
- El `f:viewAction` llama a `portalLoginBean.entrada()`.
- `PortalLoginBean#entrada()` redirige a `/portal/inicio` solo si `PortalSesion.isAutenticado()` es verdadero; en caso contrario redirige a `/portal/login`.
- La decision no depende del login administrativo, por lo que no reproduce el bug de mezcla de sesiones reportado anteriormente.
- No introduce cambios de datos ni backend sensible; es un entrypoint de redireccion.

## Riesgos

Riesgo bajo: cambio localizado en entrada del portal.

## Pruebas Revisadas

- [x] Revision estatica de `portal/index.xhtml`.
- [x] Revision estatica de `PortalLoginBean#entrada()`.
- [x] Busqueda de referencias relacionadas con `PortalSesion` y rutas `/portal`.
- [x] `mvn -q -pl sginmo-web -am clean package` ejecutado desde `Desarrollo` con resultado EXIT 0.

## Pruebas Faltantes

- [ ] Prueba manual en VPS de `/sginmo-web/portal/` sin sesion.
- [ ] Prueba manual en VPS de `/sginmo-web/portal/` con sesion de portal activa.
