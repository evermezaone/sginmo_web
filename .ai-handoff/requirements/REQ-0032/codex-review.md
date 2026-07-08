# REQ-0032 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-08
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Hallazgos

### Bloqueantes

- Ninguno pendiente.

### Observaciones cerradas

- Obs 241 cerrada en ronda 2: `tools/deploy-vps.ps1` captura el HTTP code de `login.xhtml`, imprime el resultado y ejecuta `exit 1` si no es `200`.

- Obs 242 cerrada en ronda 2: `FlywayMigrator` ya no silencia errores de migracion; registra `severe` y relanza `IllegalStateException`, abortando el deployment si Flyway falla.

### No Bloqueantes

- El script sube a `.tmp`, luego hace `mv` a `.war`, limpia marcadores de estado y toca `.dodeploy`.
- El script espera `.deployed` y falla ante `.failed` o timeout.
- `pom.xml` incluye `flyway-core` y `flyway-database-postgresql`.
- `FlywayMigrator` es `@Startup @Singleton`, usa `java:/jdbc/SGInmoDS`, `baselineOnMigrate(true)` y `baselineVersion("21")`.
- `persistence.xml` mantiene `hibernate.hbm2ddl.auto=none`.

## Riesgos

- Sin riesgos bloqueantes detectados para el alcance de REQ-0032.

## Pruebas Revisadas

- [x] Revision estatica de `tools/deploy-vps.ps1`.
- [x] Revision estatica de `FlywayMigrator`.
- [x] Revision estatica de dependencias Flyway en `pom.xml`.
- [x] Revision estatica de `persistence.xml`.
- [x] Build: `mvn -q clean package` en `Desarrollo` con EXIT 0.

## Pruebas Faltantes

- [ ] Prueba operativa negativa con endpoint no 200.
- [ ] Prueba operativa negativa de migracion Flyway fallida.
