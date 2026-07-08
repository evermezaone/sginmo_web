# REQ-0032 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-08
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

- Obs 241: `tools/deploy-vps.ps1` verifica HTTP solo imprimiendo el codigo (`curl -s -o /dev/null -w "Verificacion VPS: HTTP %{http_code}\n"`), pero no falla si el endpoint devuelve 404/500/302 inesperado. `curl` sin `--fail` termina con exit 0 para respuestas HTTP de error, por lo que el script puede imprimir "Deploy completado." con la aplicacion rota. Impacto: el deploy no es verificable de forma atomica; puede aprobar un WAR desplegado que no responde correctamente. Solucion esperada: capturar el codigo HTTP y hacer `exit 1` si no es `200` (o usar `curl --fail` y validar explicitamente), manteniendo el mensaje claro del codigo recibido.

- Obs 242: `FlywayMigrator.migrar()` atrapa `Exception` y solo loguea warning, permitiendo que la aplicacion arranque aunque Flyway no pueda migrar. El REQ dice que V22+ se auto-aplican y que el deploy/migraciones automaticas funcionan; si una migracion falla, continuar con esquema viejo rompe el contrato y puede ocultar errores hasta runtime. Impacto: deploy exitoso con BD no migrada. Solucion esperada: fallar ruidosamente en arranque/deploy ante error Flyway (por ejemplo relanzar `IllegalStateException` luego de loguear), salvo que exista una decision explicita de operar en modo degradado.

### No Bloqueantes

- El script sube a `.tmp`, luego hace `mv` a `.war`, limpia marcadores de estado y toca `.dodeploy`.
- El script espera `.deployed` y falla ante `.failed` o timeout.
- `pom.xml` incluye `flyway-core` y `flyway-database-postgresql`.
- `FlywayMigrator` es `@Startup @Singleton`, usa `java:/jdbc/SGInmoDS`, `baselineOnMigrate(true)` y `baselineVersion("21")`.
- `persistence.xml` mantiene `hibernate.hbm2ddl.auto=none`.

## Riesgos

- Falsos positivos de deploy: WAR desplegado pero aplicacion HTTP rota.
- Falsos positivos de migracion: app arrancada con esquema no actualizado.

## Pruebas Revisadas

- [x] Revision estatica de `tools/deploy-vps.ps1`.
- [x] Revision estatica de `FlywayMigrator`.
- [x] Revision estatica de dependencias Flyway en `pom.xml`.
- [x] Revision estatica de `persistence.xml`.

## Pruebas Faltantes

- [ ] Reejecutar `mvn -q clean package` luego de corregir.
- [ ] Prueba de deploy con endpoint 500/404 simulado o URL invalida: el script debe fallar.
- [ ] Prueba de migracion Flyway fallida: el deploy/arranque debe fallar ruidosamente.
