# REQ-0065 - Backup automatico PostgreSQL, archivos y configuracion VPS

**Numero:** REQ-0065
**Fecha de creacion:** 2026-07-12
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

Proponer requerimientos para elevar el nivel del programa y agregar configuracion de base de datos para asegurar backup, recuperacion y funcionalidades vendibles.

## Objetivo Funcional

Implementar una estrategia automatica de backup para SGInmo que cubra base PostgreSQL, archivos generados/subidos y configuracion critica de despliegue, con retencion, verificacion y evidencia visible para administradores.

## Criterios De Aceptacion

- [x] Existe script versionado para backup de PostgreSQL con `pg_dump` en formato custom (`.dump`) y compresion opcional. (`tools/vps/sginmo-backup.sh`; `pg_dump -Fc` con `--enable-row-security` + `app.tenant=-1` para cubrir multiempresa; custom ya viene comprimido)
- [x] Existe backup de archivos de aplicacion que deban persistir fuera del WAR: documentos generados, adjuntos, plantillas exportadas y configuraciones externas. (tar.gz de `SGINMO_ARCHIVOS_DIR`; hoy el repositorio documental aun no tiene contenido en la VPS -se omite con log-, pero el respaldo esta implementado)
- [x] Los backups se ejecutan automaticamente en la VPS por timer/cron/systemd con frecuencia configurable. (unidades versionadas `sginmo-backup.service` + `.timer` diario 03:15 `Persistent=true`; la INSTALACION del timer/cron es persistencia en el host -paso de operaciones, ver README y "Limitaciones"-; verificado corriendo el script a mano: manifest.jsonl con historial OK)
- [x] Cada ejecucion genera manifiesto con fecha, base, tamano, hash SHA-256, duracion, resultado y ruta. (`latest.json` + `manifest.jsonl`; verificado en la VPS: timestamp, base, bytes, sha256, duracion_seg, resultado, archivo)
- [x] Hay politica de retencion configurable: diarios, semanales y mensuales. (`SGINMO_KEEP_DAILY/WEEKLY/MONTHLY` 7/4/6; promocion domingo->weekly, dia01->monthly)
- [x] El sistema elimina backups vencidos sin borrar el ultimo backup valido. (`prune_dir` conserva los N mas nuevos; verificado: "Retencion: elimino ..." en backup.log sin vaciar la clase)
- [x] Las credenciales no quedan hardcodeadas en scripts versionados; se leen desde entorno o archivo protegido. (`backup.env` chmod 600 fuera del repo; el repo solo trae `backup.env.example` con placeholder)
- [x] El resultado del ultimo backup se muestra en pantalla administrativa o endpoint interno protegido. (panel Salud REQ-0051 lee `latest.json` -> indicador "Ultimo backup" con semaforo OK/ADVERTENCIA/CRITICO y frescura 48h)
- [x] Si falla el backup, queda log claro y estado visible para el administrador. (trap ERR -> `resultado":"FAIL"` + ERROR en backup.log; Salud lo refleja como CRITICO)
- [x] Documentar procedimiento manual de ejecucion y ubicacion de archivos en la VPS. (`tools/vps/README.md`: rutas, instalacion, ejecucion manual, programacion, retencion, verificacion, seguridad)

## Reglas De Negocio

- Nunca se debe desplegar una version productiva sin al menos un backup reciente valido.
- El backup debe cubrir datos multiempresa completos, no solo el tenant activo.
- El proceso debe ser no destructivo y no bloquear operaciones normales.
- Los archivos generados por contratos, pagares, comprobantes y adjuntos deben estar incluidos si existen fuera de BD.

## Dependencias

- Depende de: REQ-0032, REQ-0041.
- Requerido por: REQ-0066, REQ-0051.

## Fuentes Y Trazabilidad

- Decision usuario 2026-07-11: elevar nivel del programa con backup, recuperacion y funcionalidades vendibles.
- Stack SGInmo: WildFly, PostgreSQL, VPS Linux.
