# REQ-0101 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-16 09:46 -03:00
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Hallazgos

### Bloqueantes

- Ninguno.

### No Bloqueantes

- Ninguno.

## Riesgos

Ninguno identificado.

## Pruebas Revisadas

- [x] Revision estatica de `WEB-INF/portal-acceso.xhtml`: `.campo-a .ui-password` queda `display:block; width:100%`.
- [x] Revision estatica de input interno: `.campo-a .ui-password input` queda `width:100%; box-sizing:border-box`.
- [x] Revision de `portal/login.xhtml`: el `p:password` con `toggleMask=true` sigue igual; el cambio es solo CSS del template.
- [x] Build local: `mvn -q -f Desarrollo\pom.xml -pl sginmo-web -am clean package` EXIT 0.

## Pruebas Faltantes

- [ ] Prueba visual manual en navegador/VPS.
