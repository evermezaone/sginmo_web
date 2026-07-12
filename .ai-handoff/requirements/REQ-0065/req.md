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

- [ ] Existe script versionado para backup de PostgreSQL con `pg_dump` en formato custom (`.dump`) y compresion opcional.
- [ ] Existe backup de archivos de aplicacion que deban persistir fuera del WAR: documentos generados, adjuntos, plantillas exportadas y configuraciones externas.
- [ ] Los backups se ejecutan automaticamente en la VPS por timer/cron/systemd con frecuencia configurable.
- [ ] Cada ejecucion genera manifiesto con fecha, base, tamano, hash SHA-256, duracion, resultado y ruta.
- [ ] Hay politica de retencion configurable: diarios, semanales y mensuales.
- [ ] El sistema elimina backups vencidos sin borrar el ultimo backup valido.
- [ ] Las credenciales no quedan hardcodeadas en scripts versionados; se leen desde entorno o archivo protegido.
- [ ] El resultado del ultimo backup se muestra en pantalla administrativa o endpoint interno protegido.
- [ ] Si falla el backup, queda log claro y estado visible para el administrador.
- [ ] Documentar procedimiento manual de ejecucion y ubicacion de archivos en la VPS.

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
