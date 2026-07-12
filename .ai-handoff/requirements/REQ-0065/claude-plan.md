# REQ-0065 - Plan De Implementacion

**Estado:** APROBADO_PARA_DESARROLLO
**Fecha:** 2026-07-12

## Estrategia

Kit de backup en `tools/vps/`: script bash (pg_dump custom + RLS, tar de archivos, manifiesto,
retencion por clases) + unidades systemd + plantilla de config sin secretos. La visibilidad la
aporta el panel Salud (REQ-0051) que lee `latest.json`. La programacion (timer/cron) es un paso
de operaciones en el host (persistencia); se versiona la unidad y se documenta la instalacion.

## Archivos A Modificar

| Archivo | Cambio |
|---|---|
| tools/vps/sginmo-backup.sh | NUEVO — script de backup + manifiesto + retencion |
| tools/vps/sginmo-backup.service/.timer | NUEVO — unidades systemd |
| tools/vps/backup.env.example | NUEVO — plantilla de config (sin clave) |
| tools/vps/README.md | NUEVO — runbook operativo |
| servicio/SaludService.java | indicador "Ultimo backup" que lee latest.json (REQ-0051) |

## Pruebas Previstas

- [ ] Script corre en la VPS y genera latest.json + manifest.jsonl con sha256
- [ ] Retencion poda sin vaciar la clase; promocion semanal/mensual
- [ ] Panel Salud muestra el indicador de backup

## Riesgos

- Corre pg_dump en prod: solo lectura, no bloquea; se ejecuta a mano (no via metodo de deploy).
- Instalar timer = persistencia en host: se deja documentado como paso de operaciones.

## Cambios De Datos

Sin cambios de esquema. Genera artefactos de backup en el filesystem de la VPS.
