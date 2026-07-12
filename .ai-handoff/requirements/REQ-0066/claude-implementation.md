# REQ-0066 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0066
- Tipo de cambio: configuracion/operacion (script bash de restore) + documental (runbook)
- Riesgo: medio (el simulacro crea/borra una base temporal en el host; restaurar sobre prod exige confirmacion doble)
- Archivos clave:
  - `tools/vps/sginmo-restore.sh`: restaura un `.dump` custom en una base TEMPORAL (`sginmo_restore_test`), valida Flyway + cuenta tablas criticas (con `app.tenant=-1` = SUPERADMIN, ve todos los tenants bajo RLS) y emite reporte (`latest-restore.json`/`restore-report.jsonl`). Guardia anti-prod: aborta si el destino es la BD prod salvo `--prod-confirm=SI_ESTOY_SEGURO`. Requiere `--yes` para ejecutar.
  - `docs/operacion/restore.md`: runbook con RPO/RTO, simulacro, recuperacion TOTAL (detener WildFly, resguardar, restaurar BD/archivos, iniciar, validar login), recuperacion PARCIAL y camino de rollback.
- Comandos probados:
  - `bash -n tools/vps/sginmo-restore.sh`: SYNTAX OK.
  - En la VPS: `sginmo-restore.sh --latest` (modo plan): carga backup.env, elige el ultimo dump, describe el plan. OK.
- Cambios de datos: no toca el esquema de prod; el simulacro opera sobre una base temporal aislada.
- Cambios de entorno: reutiliza `backup.env` (REQ-0065); no agrega variables nuevas.
- Impacto LLM/tokens: no.
- Decision esperada: aprobar; revisar la guardia anti-prod y que no haya secretos en logs/reporte.
- Notas para auditor:
  - Guardia anti-prod: sin `--prod-confirm=SI_ESTOY_SEGURO` nunca toca la BD `sginmo`; sin `--yes` solo imprime el plan.
  - Validacion con `app.tenant=-1` para que RLS deje contar TODOS los tenants (no solo el GLOBAL).
  - La clave sale de `backup.env` (chmod 600), nunca se imprime; el reporte solo trae rutas/conteos/duracion.

## Resumen Funcional

SGInmo tiene un procedimiento reproducible de recuperacion: un simulacro que restaura el ultimo
backup en una base temporal y valida integridad, y un runbook para recuperar el sistema completo o
un dato puntual, con objetivos RPO/RTO definidos.

## Resumen Tecnico

Script bash fail-safe (`set -euo pipefail` + trap ERR -> reporte FAIL). createdb temporal +
pg_restore `--enable-row-security` + validaciones psql. Reporte JSON sin dependencias. Runbook en
`docs/operacion/`.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| tools/vps/sginmo-restore.sh | NUEVO — restore/simulacro a base temporal |
| docs/operacion/restore.md | NUEVO — runbook de recuperacion |

## Cambios De Datos

Sin cambios de esquema (base temporal aislada).

## Variables De Entorno

Reutiliza `backup.env` (REQ-0065). Sin variables nuevas.

## Pruebas Ejecutadas

`bash -n` OK; modo plan en la VPS OK (carga config, elige ultimo dump). Ver test-plan.

## Pruebas Manuales Sugeridas

1. `sginmo-restore.sh --latest --yes` -> revisar `latest-restore.json` (conteos > 0, resultado OK).
2. Intentar apuntar a `sginmo` sin `--prod-confirm` -> debe ABORTAR.
3. Ejecutar la "Recuperacion PARCIAL" del runbook con `--keep` e inspeccionar la base temporal.

## Limitaciones Conocidas (transparencia)

- La corrida REAL del simulacro (`--yes`: crea/borra base temporal) es escritura en el host de
  PostgreSQL y queda a operaciones (bloqueo de sandbox). El script + validaciones estan versionados
  y probados en modo plan; el runbook trae una tabla para registrar la primera corrida real.

## Riesgos Conocidos

- Restaurar sobre prod es destructivo: mitigado con doble confirmacion (`--yes` + `--prod-confirm`).
