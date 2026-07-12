# Backup automatico de SGInmo (REQ-0065)

Kit de respaldo para la VPS: base PostgreSQL (multiempresa/RLS) + repositorio documental +
manifiesto verificable, con retencion por clases y visibilidad en el panel de Salud.

## Archivos del kit

| Archivo | Rol |
|---|---|
| `sginmo-backup.sh` | Script de backup (pg_dump custom `.dump` + tar de archivos + manifiesto + retencion). |
| `backup.env.example` | Plantilla de configuracion. Se copia a un archivo **protegido, NO versionado** con la clave real. |
| `sginmo-backup.service` | Unidad systemd (oneshot) que ejecuta el script. |
| `sginmo-backup.timer` | Timer systemd diario 03:15 con `Persistent=true` (recupera ejecuciones perdidas). |

## Ubicaciones en la VPS (host `sginmo-vps`, usuario `edm`)

- Script:        `/home/edm/sginmo/bin/sginmo-backup.sh`
- Config (clave):`/home/edm/sginmo/backup.env`  (chmod 600, dueno edm) — **fuera del repo**
- Destino:       `/home/edm/backups/{daily,weekly,monthly}/`
- Manifiesto:    `/home/edm/backups/latest.json` (ultimo) y `/home/edm/backups/manifest.jsonl` (historico)
- Log:           `/home/edm/backups/backup.log`
- Documental:    `/home/edm/sginmo/archivos/` (se respalda si existe y tiene contenido)

## Procedimiento manual

Instalar/actualizar el script y la config (desde la estacion):

```bash
scp tools/vps/sginmo-backup.sh sginmo-vps:sginmo/bin/sginmo-backup.sh
ssh sginmo-vps 'chmod +x ~/sginmo/bin/sginmo-backup.sh'
# Config protegida (una sola vez; poner la clave real):
scp tools/vps/backup.env.example sginmo-vps:sginmo/backup.env   # luego editar y chmod 600
ssh sginmo-vps 'chmod 600 ~/sginmo/backup.env'
```

Ejecutar a mano:

```bash
ssh sginmo-vps '~/sginmo/bin/sginmo-backup.sh --dry-run'   # simula, no vuelca la BD
ssh sginmo-vps '~/sginmo/bin/sginmo-backup.sh'             # backup real
ssh sginmo-vps 'cat ~/backups/latest.json'                 # verificar resultado
```

## Programacion automatica (systemd) — paso de OPERACIONES

> Instalar un timer/cron es persistencia en el host y queda **fuera** del metodo de deploy
> automatizado. Debe hacerlo un operador con sudo (o con systemd de usuario + linger). Pasos:

```bash
# Opcion A: systemd de sistema (requiere sudo)
sudo cp tools/vps/sginmo-backup.service /etc/systemd/system/
sudo cp tools/vps/sginmo-backup.timer   /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable --now sginmo-backup.timer
systemctl list-timers sginmo-backup.timer

# Opcion B: systemd de usuario (sin sudo)
mkdir -p ~/.config/systemd/user
cp tools/vps/sginmo-backup.service ~/.config/systemd/user/
cp tools/vps/sginmo-backup.timer   ~/.config/systemd/user/
systemctl --user daemon-reload
systemctl --user enable --now sginmo-backup.timer
loginctl enable-linger edm   # para que corra sin sesion abierta

# Opcion C: cron
crontab -e   # agregar:  15 3 * * *  /home/edm/sginmo/bin/sginmo-backup.sh
```

## Retencion

Configurable en `backup.env` (`SGINMO_KEEP_DAILY/WEEKLY/MONTHLY`; por defecto 7/4/6).
- Diarios en `daily/`; el domingo se promueve a `weekly/`; el dia 01 a `monthly/`.
- La poda conserva los N mas nuevos por clase y **nunca** borra el ultimo backup valido.

## Verificacion e integridad

- Cada `.dump` tiene su sidecar `.sha256`. Verificar: `sha256sum -c archivo.dump.sha256`.
- El panel **Salud** (REQ-0051) lee `latest.json` y muestra el indicador "Ultimo backup"
  (OK/ADVERTENCIA/CRITICO; advierte si supera 48 h o si el resultado no es OK).
- Si el script falla, deja `resultado":"FAIL"` en el manifiesto, linea de ERROR en el log y
  Salud pasa a CRITICO.

## Seguridad

- La clave de la BD vive solo en `backup.env` (chmod 600), nunca en el repo ni en la unidad systemd.
- El manifiesto expone rutas y hashes, nunca credenciales.
- El backup es de solo lectura sobre la BD (`pg_dump`), no bloquea operaciones.
- Cubre datos multiempresa completos: `--enable-row-security` + `app.tenant=-1` (superusuario logico).
