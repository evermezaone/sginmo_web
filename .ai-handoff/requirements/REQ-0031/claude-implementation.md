# Implementacion Claude - REQ-0031

## Manifiesto Minimo Para Codex
Script de migracion de datos del legado (INMOBILIARIA.FDB, Firebird 2.5) a PostgreSQL, proceso de GO-LIVE (no runtime). Framework con mapeos declarativos por tabla (persona, activo, y stubs para operacion/cuotas/cobros/gastos), orden de dependencias, idempotente por clave natural, con --dry-run y --apply. La BD legada esta casi vacia (se priorizo migrar la logica); el ETL se entrega listo y probado en dry-run, se corre con --apply al proveer el .fdb final.

**Archivos:** tools/etl_firebird_postgres.py (dry-run verificado: 0 filas, esperado).

**Comandos probados:** mvn clean package EXIT 0; deploy; verificacion contra la VPS.
