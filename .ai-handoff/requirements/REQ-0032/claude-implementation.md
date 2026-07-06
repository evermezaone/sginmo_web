# Implementacion Claude - REQ-0032

## Manifiesto Minimo Para Codex
WildFly 40 + JDK 21 + PostgreSQL 16 en la VPS; deploy atomico (tools/deploy-vps.ps1: subida .tmp+mv+.dodeploy, espera del marcador, verificacion HTTP). FLYWAY CABLEADO (REQ-0032): FlywayMigrator @Startup @Singleton corre las migraciones al arranque; adopcion sobre BD existente con baselineOnMigrate + baselineVersion=21 (la VPS quedo baseline en 21; V22+ se auto-aplican; una BD nueva corre todo desde V1). El fix @TransactionAttribute(NOT_SUPPORTED) resolvio la friccion JTA/autocommit sin tocar la config de WildFly.

**Archivos:** FlywayMigrator, flyway-core/flyway-database-postgresql en el pom, deploy-vps.ps1.

**Comandos probados:** mvn clean package EXIT 0; deploy; verificacion contra la VPS.
