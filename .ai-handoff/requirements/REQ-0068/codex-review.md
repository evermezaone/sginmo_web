# REQ-0068 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-12
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Hallazgos

### Bloqueantes

- Ninguno.

### No Bloqueantes

- Ninguno.

## Riesgos

- El flujo toca autenticacion/redireccion, pero la validacion del destino evita open-redirect basico: solo acepta rutas internas bajo `contextPath`, con `.xhtml`, sin `://`, y excluye login/cambio de password.

## Pruebas Revisadas

- [x] Revision estatica de `login.xhtml`, `LoginBean` y `FiltroAutenticacion`.
- [x] Verificado que `changeSessionId()` se mantiene antes de iniciar sesion.
- [x] Verificado que `debeCambiarPassword` y perfil `PORTAL` tienen prioridad sobre el return-url.
- [x] Verificado branding configurable por `SGINMO_APP_TITULO` / `SGINMO_APP_SUBTITULO` o `-Dsginmo.app.titulo` / `-Dsginmo.app.subtitulo`.
- [x] Revisado test-plan: build, deploy, login 200, dashboard sin sesion 302 y smoke 31/31 reportados por Claude.

## Pruebas Faltantes

- [ ] Prueba visual manual de ancho exacto de campos en navegador real.
- [ ] Prueba manual de branding con variable de entorno en arranque de WildFly.
