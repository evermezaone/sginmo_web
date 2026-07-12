# REQ-0066 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `bash -n tools/vps/sginmo-restore.sh` | sin errores de sintaxis | OK |
| T02 | `sginmo-restore.sh --latest` (modo plan, en la VPS) | carga config, elige ultimo dump, imprime plan | OK — dump `sginmo_20260712_052338.dump`, destino `sginmo_restore_test` |
| T03 | Guardia anti-prod (destino = sginmo, sin --prod-confirm) | ABORTA con mensaje | verificado por codigo (rama de guardia) |
| T04 | Reporte de simulacro | latest-restore.json con timestamp/dump/resultado/conteos/duracion | estructura definida; se puebla al correr con --yes |

## Pruebas Manuales (operaciones)

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | `sginmo-restore.sh --latest --yes` | base temporal restaurada; conteos > 0; resultado OK; base temporal borrada | pendiente (corrida real = escritura en host PG, a operaciones) |
| M02 | Restore sobre prod sin `--prod-confirm` | ABORTA | pendiente |
| M03 | Recuperacion PARCIAL con `--keep` | base temporal inspeccionable | pendiente |

## Datos De Prueba

Backups reales en `~/backups/daily/` (generados por REQ-0065) y `backup.env` en la VPS.

## Nota De Alcance

La ejecucion real del simulacro crea y borra una base temporal en el servidor PostgreSQL de
produccion (operacion de escritura en el host). Queda a operaciones; el script y las validaciones
estan versionados y probados en modo plan. El runbook incluye una tabla para registrar la primera
corrida real.
