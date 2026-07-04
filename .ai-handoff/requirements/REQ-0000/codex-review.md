# REQ-0000 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-04
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

1. La compuerta obligatoria `npm run handoff:check` falla contra la BD real.
   - Problema: `tools/handoff-check.js` consulta `AUDITORIA_OBSERVACION.Req` y `Estado = 'pendiente'`, pero la tabla real usa `IdReq` y estados `abierta/corregida/descartada/diferida`.
   - Evidencia: `npm run handoff:check` devuelve `HANDOFF CHECK: FAIL - AUDITORIA_OBSERVACION: no se pudo consultar observaciones pendientes (Unknown column 'Req' in 'SELECT')`.
   - Impacto: Claude puede derivar o reenviar REQs sin que la compuerta detecte observaciones abiertas, o la compuerta queda inutilizable para todo el flujo. Esto bloquea el contrato BD-first del workflow.
   - Solucion esperada: corregir `tools/handoff-check.js` y `tools/handoff-ready.js` para consultar observaciones mediante `AUDITORIA_OBSERVACION.IdReq -> REQ.IdReq`, filtrando por `PROYECTO.Codigo`, `REQ.Codigo` y `AUDITORIA_OBSERVACION.Estado = 'abierta'`. Reejecutar `npm run handoff:check` con `EXIT:0`.

### No Bloqueantes

- `claude-plan.md` quedo como plantilla, aunque `claude-implementation.md`, `test-plan.md` y `preaudit-checklist.md` si contienen evidencia suficiente. No lo marco bloqueante porque la entrega ya tiene manifiesto y pruebas verificables.

## Verificaciones Realizadas

- `git remote -v`: remoto `https://github.com/evermezaone/sginmo_web.git`.
- `git log --oneline --decorate -5`: `HEAD -> main, origin/main`.
- `git ls-files`: no incluye `.env`, `tmp_my.cnf`, `mysql20*.sql`, claves SSH ni archivos `.pem/.ppk`.
- `git grep -n -i "password|passwd|VPS_PASS|tmp_my|PROJECT_DB_PASSWORD|BEGIN OPENSSH PRIVATE KEY"`: sin secretos versionados; solo referencias documentales o lectura desde variables de entorno.
- `ssh sginmo-vps "echo CONEXION_OK; whoami; hostname; uname -srm"`: `CONEXION_OK`, usuario `edm`, host `vmi3296290`.
- `DESCRIBE AUDITORIA_OBSERVACION`: confirma columnas `IdReq` y estado enum `abierta/corregida/descartada/diferida`.
- `npm run handoff:check`: falla por columna inexistente `Req`.

## Resultado

Se rechaza hasta corregir la compuerta Node del handoff y demostrar `npm run handoff:check` con `EXIT:0`.
