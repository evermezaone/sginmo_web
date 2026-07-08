# REQ-0031 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-08
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

- Obs 239: el ETL declara migrar `persona (+ persona_fisica/juridica) + persona_rol`, pero `_insertar()` solo inserta en la tabla base `persona`. No hay carga de `persona_fisica`, `persona_juridica` ni `persona_rol`; tampoco hay mapeos/stubs ejecutables para `operacion`, `cronograma_cuota`, `cobro`, `cobro_detalle` o `ingreso_egreso`. Impacto: un `--apply` con datos reales deja personas incompletas y no migra las tablas transaccionales prometidas por el REQ. Solucion esperada: implementar al menos los pasos declarativos/stubs seguros de todas las familias exigidas, y para personas crear la base, recuperar su id por `numero_documento` y poblar especializacion y roles segun corresponda.

- Obs 240: la idempotencia por clave natural no esta garantizada para activos. El REQ promete idempotencia por `nombre+tipo`, pero la tabla `activo` no tiene `UNIQUE(nombre, tipo_codigo)` ni el script hace lookup/update por esa clave; usa `INSERT ... ON CONFLICT DO NOTHING` sin conflicto natural aplicable. Impacto: al re-correr `--apply`, los activos se duplican porque no hay conflicto que dispare `DO NOTHING`. Solucion esperada: resolver idempotencia explicitamente por lookup/upsert de clave natural en Python o crear una restriccion/indice unico compatible con el dominio antes de confiar en `ON CONFLICT`.

### No Bloqueantes

- El script tiene modo `--dry-run` y `--apply`.
- Lee `.env` y contempla `LEGACY_FDB_PATH`.
- Descubre tablas reales de Firebird via `RDB$RELATIONS`.
- El orden declarado de dependencias es razonable para el go-live.

## Riesgos

- Dry-run con 0 filas no valida el camino de escritura real.
- Un apply sobre la FDB final puede fallar o dejar una migracion parcial/inconsistente.

## Pruebas Revisadas

- [x] Revision estatica de `tools/etl_firebird_postgres.py`.
- [x] Comparacion contra esquema PostgreSQL `persona`, `persona_fisica`, `persona_juridica`, `persona_rol` y `activo`.
- [x] Comparacion contra alcance de `REQ-0031`.
- [ ] Dry-run local: no ejecutado, `python tools\etl_firebird_postgres.py --dry-run` fallo en este entorno con `python.exe` inaccesible.

## Pruebas Faltantes

- [ ] Reejecutar dry-run con un runtime Python funcional.
- [ ] Prueba de idempotencia con fixture minimo: dos ejecuciones `--apply` no deben duplicar personas/activos.
- [ ] Prueba de persona juridica/fisica con rol: debe poblar base, especializacion y `persona_rol`.
