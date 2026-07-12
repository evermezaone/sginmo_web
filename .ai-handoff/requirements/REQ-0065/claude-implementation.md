# REQ-0065 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0065
- Tipo de cambio: configuracion/operacion (script bash + unidades systemd + runbook) + backend liviano (indicador Salud)
- Riesgo: medio (corre pg_dump en prod -solo lectura-; instalar timer = persistencia en host, se deja como paso de operaciones)
- Archivos clave:
  - `tools/vps/sginmo-backup.sh`: backup pg_dump custom (`-Fc --enable-row-security`, `app.tenant=-1` para multiempresa) + tar del repositorio documental + manifiesto (latest.json/manifest.jsonl con sha256/duracion/resultado) + retencion daily/weekly/monthly (nunca borra el ultimo) + trap ERR (FAIL + log).
  - `tools/vps/sginmo-backup.service` + `.timer`: unidad systemd diaria 03:15 `Persistent=true`.
  - `tools/vps/backup.env.example`: plantilla; la clave real vive en `backup.env` chmod 600 fuera del repo.
  - `tools/vps/README.md`: runbook (rutas, instalacion, ejecucion manual, programacion, retencion, verificacion, seguridad).
  - `servicio/SaludService.java` (ya de REQ-0051): indicador "Ultimo backup" que lee latest.json y da semaforo/frescura 48h.
- Comandos probados (VPS `sginmo-vps`, usuario edm):
  - `~/sginmo/bin/sginmo-backup.sh`: OK; `latest.json` `resultado":"OK"`, dump ~400 KB con `.sha256`.
  - `manifest.jsonl`: historial de corridas OK (fecha/base/bytes/sha256/duracion/resultado).
  - `backup.log`: "Backup OK en 1s", "Retencion: elimino ...", "Promocion semanal (domingo)".
  - Retencion verificada: poda daily/weekly sin vaciar la clase.
- Cambios de datos: no (no toca esquema; genera artefactos de backup en el filesystem de la VPS).
- Cambios de entorno: si — `backup.env` (protegido, fuera del repo) con PGPASSWORD y rutas/retencion.
- Impacto LLM/tokens: no.
- Decision esperada: aprobar; revisar el punto de PERSISTENCIA (instalacion del timer) que queda a operaciones.
- Notas para auditor:
  - Sin secretos versionados: el repo solo trae `backup.env.example` (placeholder). La clave real esta en la VPS (chmod 600).
  - pg_dump es solo lectura y no bloquea; cubre multiempresa completo (RLS + tenant -1).
  - El manifiesto no expone credenciales (solo rutas y hashes).
  - La INSTALACION del timer/cron no se automatiza desde aca (persistencia en host); esta versionada y documentada para operaciones.

## Resumen Funcional

SGInmo cuenta con respaldo verificable de su base y archivos: dumps diarios con retencion
(diaria/semanal/mensual), manifiesto con hash y resultado, y visibilidad del ultimo backup en el
panel de Salud (indicador con semaforo). Si un backup falla, el administrador lo ve como CRITICO.

## Resumen Tecnico

Script bash idempotente y fail-safe (`set -euo pipefail` + trap ERR). pg_dump `-Fc` con RLS y
`app.tenant=-1`. Manifiesto JSON sin dependencias. Retencion por clase con promocion por calendario.
Config y clave fuera del repo. El indicador de Salud (REQ-0051) ya consume `latest.json`.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| tools/vps/sginmo-backup.sh | NUEVO — script de backup |
| tools/vps/sginmo-backup.service | NUEVO — unidad systemd |
| tools/vps/sginmo-backup.timer | NUEVO — timer diario 03:15 |
| tools/vps/backup.env.example | NUEVO — plantilla de config sin clave |
| tools/vps/README.md | NUEVO — runbook operativo |

## Cambios De Datos

Sin cambios de esquema.

## Variables De Entorno

`backup.env` (fuera del repo, chmod 600): PGHOST/PGPORT/PGDATABASE/PGUSER/PGPASSWORD,
SGINMO_BACKUP_ROOT, SGINMO_ARCHIVOS_DIR, SGINMO_KEEP_DAILY/WEEKLY/MONTHLY.
El backend Salud reconoce `SGINMO_BACKUP_MANIFEST` (default `~/backups/latest.json`).

## Pruebas Ejecutadas

Ejecucion real del script en la VPS (multiples corridas OK); manifiesto/retencion/promocion
verificados por log y latest.json. Ver test-plan.

## Pruebas Manuales Sugeridas

1. Instalar el timer (operaciones) y confirmar `systemctl list-timers sginmo-backup.timer`.
2. Forzar un fallo (clave mala) -> manifiesto FAIL + Salud CRITICO.
3. `sha256sum -c archivo.dump.sha256` sobre un dump para validar integridad.

## Limitaciones Conocidas (transparencia)

- La INSTALACION del timer/cron (persistencia en el host) queda a operaciones; versionada y documentada.
- El repositorio documental (`~/sginmo/archivos`) aun no tiene contenido en la VPS: el respaldo de
  archivos esta implementado pero hoy se omite con log ("aun no configurado").

## Riesgos Conocidos

- Corre pg_dump en prod (solo lectura, no bloquea). Programacion pendiente de operaciones.
