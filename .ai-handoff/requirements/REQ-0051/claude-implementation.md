# REQ-0051 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0051
- Tipo de cambio: UI + backend + BD (siembra de pantalla) + configuracion (sello de build)
- Riesgo: medio
- Archivos clave:
  - `sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/SaludService.java`: arma el snapshot de indicadores (solo lectura); sin @AislarTenant a proposito (lee flyway_schema_history/SELECT 1/disco/JVM/manifiesto, nada bajo RLS).
  - `sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/SaludBean.java`: backing bean @ViewScoped con verificarAcceso() y refrescar().
  - `sginmo-web/src/main/webapp/salud.xhtml`: vista con semaforos (p:tag) por indicador + sello de version.
  - `sginmo-web/src/main/webapp/WEB-INF/plantilla.xhtml`: item de menu "Salud del sistema" bajo Configuracion, rendered por `puede('salud','VER')`.
  - `sginmo-web/src/main/resources/db/migration/V32__pantalla_salud.sql`: registra la pantalla `salud` en `entidad` (lista PANTALLAS, tenant -1) con set_config app.tenant=-1.
  - `sginmo-web/pom.xml` + `src/main/resources/build-info.properties`: sello de build filtrado por Maven (version/fecha/commit).
  - `tools/smoke-test-vps.py`: agrega `salud` a la cobertura de render.
- Comandos probados:
  - `mvn -q clean package -DskipTests -DbuildCommit=5a658f5`: BUILD OK; build-info.properties filtrado (version 0.1.0-SNAPSHOT, build 2026-07-12T01:26:31Z, commit 5a658f5).
  - V32 en `BEGIN...ROLLBACK` contra la BD real: inserta 1 fila `PANTALLAS/salud`; rollback limpio.
  - Deploy a la VPS + Flyway V32 `success=t`; `SELECT` confirma pantalla sembrada.
  - `python tools/smoke-test-vps.py`: 19/19 pantallas RENDER OK, incluida `salud` (el snapshot ejecuto sin error: DB+Flyway+disco+JVM+manifiesto de backup).
- Cambios de datos: si, ver migracion V32 (solo alta de 1 fila de catalogo de pantallas; sin datos de negocio).
- Cambios de entorno: opcional `SGINMO_BACKUP_MANIFEST` (default `~/backups/latest.json`); `-DbuildCommit` en el build.
- Impacto LLM/tokens: no.
- Decision esperada: aprobar (riesgo medio, solo lectura).
- Notas para auditor:
  - Verificar que ningun indicador expone secretos/credenciales/rutas completas de secretos (solo nombres de archivo, version, %).
  - `SaludService` es @Transactional pero solo hace SELECTs; sin @AislarTenant es intencional (objetos sin RLS).
  - Gap conocido de plataforma (no de este REQ): un SUPERADMIN sin perfil ADMINISTRADOR necesita permiso `salud:VER` explicito; el perfil ADMINISTRADOR ve la pantalla sin sembrar permisos.
  - Regla de negocio "alertas criticas como evento operativo": diferida a integrar con REQ-0067 (auditoria/evento); hoy no hay canal de evento operativo. Ver test-plan.

## Resumen Funcional

Nuevo panel "Salud del sistema" (menu Configuracion) que muestra, con semaforos OK/Advertencia/
Critico: conexion y latencia a PostgreSQL, version/fecha/commit del build, estado de Flyway
(version aplicada + migraciones fallidas), espacio en disco, memoria JVM, uptime y ultimo backup.
Solo lectura; visible solo para usuarios autorizados.

## Resumen Tecnico

`SaludService.snapshot()` arma una lista de indicadores tipados con estado y detalle, y un estado
global = peor semaforo. Lecturas nativas para SELECT 1 y flyway_schema_history; `File`/`Runtime`/
`ManagementFactory` para disco/JVM/uptime; parse minimo del manifiesto JSON de backup. Version desde
`build-info.properties` filtrado por Maven (nueva infra: `<resources>` con filtering solo para ese
archivo + propiedades `maven.build.timestamp` y `buildCommit`). Bean @ViewScoped con
`f:viewAction` para cortar acceso por URL. Migracion V32 registra la pantalla en `entidad`.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| servicio/SaludService.java | NUEVO — snapshot de indicadores de salud |
| web/SaludBean.java | NUEVO — backing bean del panel |
| webapp/salud.xhtml | NUEVO — vista con semaforos |
| webapp/WEB-INF/plantilla.xhtml | item de menu "Salud del sistema" |
| resources/db/migration/V32__pantalla_salud.sql | NUEVO — registra pantalla `salud` |
| pom.xml | filtrado de build-info + timestamp/commit |
| resources/build-info.properties | NUEVO — sello de build |
| tools/smoke-test-vps.py | agrega `salud` a la cobertura |

## Cambios De Datos

V32: INSERT idempotente de 1 fila en `entidad` (lista='PANTALLAS', codigo='salud', tenant=-1). Sin
cambios en datos de negocio. Requiere `set_config('app.tenant','-1',true)` por RLS (V28).

## Variables De Entorno

- `SGINMO_BACKUP_MANIFEST` (opcional): ruta del manifiesto de backup; default `~/backups/latest.json`.
- `-DbuildCommit=<hash>` en el build (opcional): embebe el commit en el sello.

## Pruebas Ejecutadas

Ver `test-plan.md`. Resumen: build OK, V32 validada en rollback y aplicada en prod (success=t),
smoke-test 19/19 RENDER OK incluida `salud`.

## Pruebas Manuales Sugeridas

1. Entrar como admin → menu Configuracion → "Salud del sistema": ver semaforos y sello de version.
2. Detener PostgreSQL (entorno de prueba) → el indicador Base de datos debe pasar a CRITICO sin romper la pagina.

## Riesgos Conocidos

- Sin manifiesto de backup (REQ-0065 no programado aun) el indicador degrada a "sin datos"/Advertencia.
- El % de disco es de la particion del proceso WildFly (documentado en el detalle del indicador).
