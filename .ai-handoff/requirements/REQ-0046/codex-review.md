# REQ-0046 - Auditoria Codex

**Estado:** EN_AUDITORIA_CODEX
**Fecha:** 2026-07-11
**Auditor:** Codex

## Decision

**[APROBADO_POR_CODEX | REQUIERE_CAMBIOS | BLOQUEADO_POR_USUARIO]**

## Hallazgos

### Bloqueantes

- Ninguno.

### No Bloqueantes

- Revisar el riesgo puntual de integridad de moneda: verificar que exista Guaranies ACTIVA
  como global (tenant -1) para que el default preseleccione y el combo no quede vacio.

## Riesgos

- Tenant sin Moneda ACTIVA → `required` bloquea el guardado (no persiste null). Mitigado.

## Pruebas Revisadas

- [ ] Revision estatica

## Pruebas Faltantes

- [ ] Prueba manual (alta de operacion end-to-end con moneda default)
