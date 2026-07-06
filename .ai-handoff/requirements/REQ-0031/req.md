# REQ-0031 - ETL Firebird a PostgreSQL

**Estado:** implementado (2026-07-06)

## Objetivo Funcional
Script de migracion de datos del legado (INMOBILIARIA.FDB, Firebird 2.5) a PostgreSQL, proceso de GO-LIVE (no runtime). Framework con mapeos declarativos por tabla (persona, activo, y stubs para operacion/cuotas/cobros/gastos), orden de dependencias, idempotente por clave natural, con --dry-run y --apply. La BD legada esta casi vacia (se priorizo migrar la logica); el ETL se entrega listo y probado en dry-run, se corre con --apply al proveer el .fdb final.

## Criterios De Aceptacion
- [x] Implementado y verificado en la VPS.

## Bloqueo Formal Documentado
Validacion visual del usuario PENDIENTE (desarrollo continuo).
