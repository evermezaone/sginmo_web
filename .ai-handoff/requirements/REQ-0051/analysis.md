# REQ-0051 - Analisis

**Estado:** EN_ANALISIS
**Fecha:** 2026-07-12
**Analista:** Claude

## Analisis Funcional

Panel administrativo de solo lectura "Salud del sistema" para soporte/operacion. Muestra
en una sola pantalla el estado tecnico del despliegue con semaforos (OK / Advertencia /
Critico):

- Conexion a PostgreSQL y latencia aproximada (SELECT 1 cronometrado).
- Version/build desplegado, fecha de despliegue y commit si esta disponible (leidos de
  propiedades filtradas por Maven en build).
- Estado de Flyway: version actual aplicada y conteo de migraciones fallidas (success=false)
  leidas de `flyway_schema_history`.
- Ultimo backup valido y ultima prueba de restore, leidos de un manifiesto en ruta
  configurable (lo producira REQ-0065/0066). Si no existe, muestra "sin datos".
- Espacio libre en disco de la particion de la app (java.io.File.getUsableSpace) con
  umbrales configurables (advertencia/critico).
- Estado basico de WildFly/datasource: memoria JVM, uptime, datasource valido.

Solo usuarios con permiso tecnico ven la pantalla y ningun dato expone secretos, rutas
sensibles completas ni credenciales.

## Analisis Tecnico

- Pantalla JSF nueva `salud.xhtml` + `SaludBean` (@Named @ViewScoped) + `SaludService`
  (@Stateless) que arma un DTO de salud.
- Lecturas: `EntityManager` para `SELECT 1` y `flyway_schema_history`; `Runtime`/`File`
  para JVM y disco; version desde propiedades filtradas por Maven.
- Backup/restore: lee manifiesto (JSON/propiedades) en ruta de entorno
  (`SGINMO_BACKUP_MANIFEST`, default bajo `~/backups`). Degradacion elegante si falta.
- Pantalla registrada en el catalogo de pantallas/permisos por migracion Flyway, asignada
  al rol tecnico/SUPERADMIN. Menu con visibilidad por `sesionUsuario.puede('salud','VER')`.
- Solo lectura: ninguna accion muta datos. Semaforos calculados en el service.

## Riesgos

| Riesgo | Probabilidad | Impacto | Mitigacion |
|---|---|---|---|
| Query de latencia bloquea si la BD esta caida | media | bajo | timeout corto + try/catch, mostrar CRITICO sin romper la pagina |
| Exponer rutas/versiones sensibles | baja | medio | mostrar solo nombres de archivo/version, nunca credenciales ni rutas completas |
| Lectura de disco/JVM difiere en contenedor vs host | media | bajo | medir la particion donde corre la JVM; documentar que es del proceso WildFly |
| Manifiesto de backup aun inexistente (REQ-0065 no hecho) | alta | bajo | criterio dice "si existen"; degradar a "sin datos" |

**Semaforo Codex:** medio

## Preguntas Abiertas

- [ ] Ninguna (bloqueos de dependencia resueltos por degradacion elegante)

## Impacto En Costos / LLM

- Aumenta tokens por mensaje: no
- Agrega llamadas extra al LLM: no
- Puede resolverse sin IA: si

## Impacto En Datos

- Requiere migracion: si (siembra de pantalla `salud` y sus permisos; sin cambios de datos de negocio)
- Tablas/colecciones afectadas: catalogo de pantallas/permisos (solo alta de la pantalla nueva)

## Recomendacion

**Desarrollar** — riesgo medio, solo lectura, alto valor para soporte y venta.
