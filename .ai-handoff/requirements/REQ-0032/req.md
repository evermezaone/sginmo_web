# REQ-0032 - Provision de servidor y deploy

**Estado:** implementado (2026-07-06)

## Objetivo Funcional
WildFly 40 + JDK 21 + PostgreSQL 16 en la VPS; deploy atomico (tools/deploy-vps.ps1: subida .tmp+mv+.dodeploy, espera del marcador, verificacion HTTP). FLYWAY CABLEADO (REQ-0032): FlywayMigrator @Startup @Singleton corre las migraciones al arranque; adopcion sobre BD existente con baselineOnMigrate + baselineVersion=21 (la VPS quedo baseline en 21; V22+ se auto-aplican; una BD nueva corre todo desde V1). El fix @TransactionAttribute(NOT_SUPPORTED) resolvio la friccion JTA/autocommit sin tocar la config de WildFly.

## Criterios De Aceptacion
- [x] Implementado y verificado en la VPS.

## Bloqueo Formal / Ops (Codex/infra)
nginx + HTTPS delante del 8080 y el hardening de produccion quedan como paso de OPS (division del proyecto: prod la maneja Codex/ops). El deploy y las migraciones automaticas ya funcionan.

## Bloqueo Formal Documentado
Validacion visual del usuario PENDIENTE (desarrollo continuo).
