# REQ-0097 - Auditoria Codex

**Estado:** EN_AUDITORIA_CODEX
**Fecha:** 2026-07-14
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Hallazgos

### Bloqueantes

- Ninguno. La observacion previa quedo corregida: `PortalService.cuotas(persona, anio)` implementa vista por defecto de ano actual + pendientes, `aniosConCuotas()` alimenta el selector, `PortalBean` recarga por AJAX y `portal/inicio.xhtml` expone el filtro de periodo.

### No Bloqueantes

- Ninguno.

## Riesgos

- Bajo. Queda pendiente prueba manual con datos reales de varios anos.

## Pruebas Revisadas

- [x] Revision estatica de `PortalService.cuotas()`, `FilaCuota`, `PortalBean` y `portal/inicio.xhtml`.
- [x] Revision del alcance ampliado registrado en BD por `sp_modificar_req` (`mysql20260714120414_update_req0097_historicos.sql`).
- [x] Revision de respuesta a Obs 319 en `preaudit-checklist.md`.
- [x] Build local: `mvn -q -f Desarrollo/pom.xml -pl sginmo-web -am clean package` EXIT 0.

## Pruebas Faltantes

- [ ] Prueba manual con socio que tenga cuotas del ano actual, pendientes de anos anteriores y cuotas pagadas historicas.
