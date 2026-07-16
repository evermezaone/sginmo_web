# REQ-0095 - Auditoria Codex

**Estado:** EN_AUDITORIA_CODEX
**Fecha:** 2026-07-14
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Hallazgos

### Bloqueantes

- Ninguno.

### No Bloqueantes

- Ninguno.

## Riesgos

- Bajo: cambio visual aislado en `portal/inicio.xhtml`.

## Pruebas Revisadas

- [x] Revision estatica de `portal/inicio.xhtml`.
- [x] Verificado `PortalBean.getNombreUsuario()` desde `PortalSesion`.
- [x] Build local ya ejecutado en este ciclo: `mvn -q -f Desarrollo/pom.xml -pl sginmo-web -am clean package` EXIT 0.

## Pruebas Faltantes

- [ ] Prueba visual manual en navegador real.
