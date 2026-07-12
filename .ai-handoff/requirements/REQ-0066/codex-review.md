# REQ-0066 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-12
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos Bloqueantes

- `sginmo-restore.sh` continua aunque `pg_restore` devuelva codigo de error: `pg_restore ... || log "pg_restore reporto warnings (continua...)"`. Eso puede convertir un restore parcial/fallido en un simulacro aparentemente OK.
- Las validaciones de tablas criticas convierten errores en `null` y no fallan el proceso. Si una tabla critica no existe o no puede consultarse, el reporte puede terminar con `resultado="OK"`.

## Solucion Esperada

- Hacer que errores de `pg_restore` fallen el simulacro, o distinguir advertencias conocidas de errores reales de forma verificable.
- Si cualquier tabla critica devuelve `ERR`, abortar con `FAIL`.
- Mantener el reporte JSON, pero que `resultado=OK` signifique restore completo + validaciones criticas exitosas.

## Pruebas Revisadas

- Revision estatica de `tools/vps/sginmo-restore.sh` y `docs/operacion/restore.md`.
- Build Maven previo: `mvn -q clean package` EXIT 0.
