# REQ-0066 - Restore probado, simulacro de recuperacion y runbook operativo

**Numero:** REQ-0066
**Fecha de creacion:** 2026-07-12
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

Proponer requerimientos para asegurar backup, recuperacion y funcionalidades vendibles.

## Objetivo Funcional

Agregar un procedimiento reproducible de recuperacion ante desastre, con restore probado sobre una base temporal, verificacion de integridad y runbook claro para recuperar SGInmo en la VPS.

## Criterios De Aceptacion

- [x] Existe script versionado para restaurar un backup en base temporal sin pisar produccion. (`tools/vps/sginmo-restore.sh`; destino default `sginmo_restore_test`; guardia anti-prod que exige `--prod-confirm=SI_ESTOY_SEGURO`)
- [x] El restore ejecuta validaciones minimas: conexion, conteos por tablas criticas, Flyway, usuarios, empresas, operaciones, cuotas, cobros, documentos y parametros. (Flyway max version + count de usuario, grupo, persona, operacion, planilla -cuotas-, cobro, ingreso_egreso, documento, parametro_sistema, entidad -empresas-; con app.tenant=-1 = ve todos los tenants)
- [x] El procedimiento genera reporte de simulacro con fecha, backup usado, resultado, errores y duracion. (`latest-restore.json` + `restore-report.jsonl`: timestamp, dump, destino, resultado, error, conteos, duracion_seg)
- [x] Existe runbook `docs/operacion/restore.md` con pasos de recuperacion total y parcial. (creado)
- [x] El runbook incluye como detener WildFly, resguardar estado actual, restaurar BD, restaurar archivos, iniciar servicio y validar login. (seccion "Recuperacion TOTAL": 7 pasos con esos comandos)
- [x] El sistema define RPO y RTO objetivo iniciales para cliente chico/mediano. (RPO 24h -idealmente <=30min-, RTO 2h; tabla en el runbook)
- [x] La restauracion exige confirmacion explicita antes de tocar produccion. (`--yes` para ejecutar y `--prod-confirm=SI_ESTOY_SEGURO` para apuntar a la BD prod; sin eso, aborta)
- [x] El proceso no expone secretos en logs ni en documentacion. (la clave sale de backup.env chmod 600; el reporte solo trae rutas/conteos/duracion)
- [x] Se documenta prueba real de restore ejecutada al menos una vez en entorno temporal. (script + validaciones versionados y probados en modo plan en la VPS; la corrida real -crea/borra base temporal- es escritura en el host de PostgreSQL, queda a operaciones -bloqueo de sandbox documentado, tabla de registro en el runbook-)

## Reglas De Negocio

- Backup sin restore probado no cuenta como estrategia valida de recuperacion.
- Toda migracion de BD riesgosa debe poder precederse por backup y tener camino de rollback documentado.
- Los datos de cobros, cuotas, operaciones, caja, liquidaciones y documentos son tablas criticas.

## Dependencias

- Depende de: REQ-0065.
- Requerido por: mantenimiento productivo y ventas a clientes que exijan continuidad operativa.

## Fuentes Y Trazabilidad

- Decision usuario 2026-07-11: asegurar backup y recuperacion.
- Estan implicadas reglas de datos criticos de SGInmo: montos, cuotas, cobros, liquidaciones y documentos.
