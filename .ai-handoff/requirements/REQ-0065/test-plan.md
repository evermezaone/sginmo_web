# REQ-0065 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas (ejecutadas en la VPS sginmo-vps)

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | Correr `~/sginmo/bin/sginmo-backup.sh` | dump custom + latest.json OK | OK — `latest.json resultado":"OK"`, dump ~400 KB |
| T02 | Manifiesto por corrida | fecha/base/bytes/sha256/duracion/resultado/ruta | OK — presentes en latest.json y manifest.jsonl |
| T03 | Sidecar de integridad | `.sha256` junto a cada `.dump` | OK — un `.sha256` por dump en daily/ |
| T04 | Retencion | poda los vencidos sin vaciar la clase | OK — log "Retencion: elimino ..." conservando N |
| T05 | Promocion por calendario | domingo->weekly, dia01->monthly | OK — log "Promocion semanal (domingo)" |
| T06 | Sin secretos versionados | repo solo con backup.env.example (placeholder) | OK — clave real solo en VPS (chmod 600) |
| T07 | Multiempresa (RLS) | pg_dump con --enable-row-security + app.tenant=-1 | OK — flags en el script; dump completo |
| T08 | Visibilidad en Salud | indicador "Ultimo backup" lee latest.json | OK — SaludService.backup() con semaforo/frescura 48h |

## Pruebas Manuales (pendientes / operaciones)

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | Instalar timer systemd | `systemctl list-timers` muestra sginmo-backup | pendiente (operaciones — persistencia en host) |
| M02 | Fallo simulado (clave mala) | manifiesto FAIL + Salud CRITICO | pendiente |
| M03 | `sha256sum -c dump.sha256` | integridad OK | pendiente |

## Datos De Prueba

Config `backup.env` en la VPS (ya presente con la clave real, chmod 600).

## Nota De Alcance

La programacion automatica (timer/cron) es un paso de operaciones (persistencia en el host) y no se
instala desde el flujo de desarrollo; las unidades systemd estan versionadas y el runbook documenta la
instalacion. La ejecucion del script y todos los artefactos de backup fueron verificados en vivo.
