# Runbook de recuperacion de SGInmo (REQ-0066)

Procedimiento reproducible para restaurar SGInmo desde un backup (REQ-0065) y para
simular la recuperacion sin tocar produccion. Complementa `tools/vps/README.md` (backup).

## Objetivos de recuperacion (iniciales)

Valores objetivo para cliente chico/mediano (revisar por contrato):

| Metrica | Objetivo | Como se cumple |
|---|---|---|
| **RPO** (perdida maxima de datos) | **24 h** (idealmente <= 30 min) | Backup diario 03:15 por timer; hoy la corrida frecuente deja RPO efectivo bajo. |
| **RTO** (tiempo maximo de recuperacion) | **2 h** | Restore de la BD (~segundos para esta base) + arranque de WildFly + validacion. |

> RPO se puede bajar aumentando la frecuencia del timer o agregando WAL archiving.
> RTO real medido en simulacro: el `pg_restore` de esta base tarda segundos; el grueso
> del RTO es el arranque de WildFly y la validacion manual.

## Simulacro de restore (NO toca produccion)

Restaura el ultimo backup en una base temporal y valida integridad. Recomendado
periodicamente (mensual) y despues de cambios de esquema riesgosos.

```bash
ssh sginmo-vps '~/sginmo/bin/sginmo-restore.sh --latest'          # plan (no ejecuta)
ssh sginmo-vps '~/sginmo/bin/sginmo-restore.sh --latest --yes'    # ejecuta el simulacro
ssh sginmo-vps 'cat ~/backups/latest-restore.json'                # reporte del simulacro
```

El script:
- crea una base temporal `sginmo_restore_test` (nunca `sginmo` salvo `--prod-confirm`),
- corre `pg_restore --enable-row-security`,
- valida Flyway y cuenta tablas criticas con `app.tenant=-1` (visibilidad SUPERADMIN = todos los tenants),
- escribe `latest-restore.json` + `restore-report.jsonl` (fecha, dump, resultado, conteos, duracion),
- elimina la base temporal al terminar (usar `--keep` para inspeccionarla).

## Recuperacion TOTAL en produccion (desastre)

> Solo ante perdida real de la BD. Exige confirmacion explicita. Deja ventana de indisponibilidad.

1. **Detener WildFly** (deja de escribir en la BD):
   ```bash
   ssh sginmo-vps '~/apps/wildfly-40.0.0.Final/bin/jboss-cli.sh --connect command=:shutdown' \
     || ssh sginmo-vps 'pkill -f wildfly'
   ```
2. **Resguardar el estado actual** antes de tocar nada (por si el desastre es recuperable):
   ```bash
   ssh sginmo-vps '~/sginmo/bin/sginmo-backup.sh'   # snapshot del estado presente
   ```
3. **Elegir el backup** a restaurar (el mas reciente valido):
   ```bash
   ssh sginmo-vps 'ls -1t ~/backups/daily/*.dump | head; cat ~/backups/latest.json'
   ```
4. **Restaurar la BD de produccion** (peligroso; confirmacion doble):
   ```bash
   ssh sginmo-vps '~/sginmo/bin/sginmo-restore.sh ~/backups/daily/<archivo>.dump sginmo \
       --recreate --keep --yes --prod-confirm=SI_ESTOY_SEGURO'
   ```
   > `--recreate` reemplaza la base `sginmo`. Verificar antes que WildFly esta detenido.
5. **Restaurar archivos** (si se respaldaron y se perdieron):
   ```bash
   ssh sginmo-vps 'tar xzf ~/backups/daily/archivos_<stamp>.tgz -C ~/sginmo/'
   ```
6. **Iniciar WildFly**:
   ```bash
   ssh sginmo-vps '~/apps/sginmo/start-wildfly.sh'   # o el metodo de arranque vigente
   ```
7. **Validar login** y salud:
   ```bash
   ssh sginmo-vps 'curl -s -o /dev/null -w "%{http_code}\n" http://localhost:8080/sginmo-web/login.xhtml'
   python tools/smoke-test-vps.py
   ```
   Entrar a la app -> panel **Salud** -> revisar Flyway, BD y "Ultimo backup".

## Recuperacion PARCIAL (una tabla / un dato)

No restaurar toda la BD. Restaurar el dump en una base temporal y copiar solo lo necesario:

```bash
ssh sginmo-vps '~/sginmo/bin/sginmo-restore.sh ~/backups/daily/<archivo>.dump sginmo_tmp --keep --yes'
# Inspeccionar/extraer la fila o tabla puntual desde sginmo_tmp y aplicarla a prod con cuidado
# (respetando RLS: setear app.tenant al del registro). Luego:
ssh sginmo-vps 'psql -d postgres -c "DROP DATABASE sginmo_tmp"'
```

## Camino de rollback ante migracion riesgosa

Antes de una migracion Flyway riesgosa: `sginmo-backup.sh` (snapshot). Si la migracion rompe algo,
aplicar la "Recuperacion TOTAL" con ese snapshot. Nunca desplegar sin un backup reciente valido.

## Seguridad

- Ni el script ni este runbook imprimen la clave: la conexion sale de `backup.env` (chmod 600).
- El reporte de simulacro (`latest-restore.json`) solo expone rutas, conteos y duracion.
- El restore sobre produccion exige `--prod-confirm=SI_ESTOY_SEGURO` (no se puede pisar prod por accidente).

## Estado de la prueba real

- Simulacro validado en modo **plan** en la VPS (carga config y selecciona el ultimo dump).
- La corrida real del simulacro (`--yes`, crea/borra base temporal) es una operacion de escritura
  en el host de PostgreSQL y debe ejecutarla un operador; el script y las validaciones estan
  versionados y listos. Registrar aqui la fecha/resultado de la primera corrida real:

  | Fecha | Dump usado | Resultado | Duracion | Notas |
  |---|---|---|---|---|
  | (pendiente) | | | | primera corrida real por operaciones |
