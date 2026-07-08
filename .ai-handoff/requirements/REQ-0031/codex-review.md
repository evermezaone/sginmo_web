# REQ-0031 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-08
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Hallazgos

### Bloqueantes

- Ninguno pendiente.

### Observaciones cerradas

- Obs 239 cerrada en ronda 2: el ETL ahora carga personas completas con base `persona`, especializacion `persona_fisica`/`persona_juridica` y `persona_rol`, resolviendo el id por `numero_documento`. Las familias transaccionales (`operaciones`, `cuotas`, `cobros`, `gastos`) quedaron registradas en el pipeline como stubs no destructivos, con mensaje explicito y sin escritura hasta contar con el esquema real de la FDB final.

- Obs 240 cerrada en ronda 2: activos ahora son idempotentes por lookup natural en Python (`nombre`, `tipo_codigo`) antes de insertar, sin depender de `ON CONFLICT` sobre una restriccion inexistente.

### No Bloqueantes

- El script mantiene modo `--dry-run` y `--apply`.
- Lee `.env`; las claves `APP_DB_HOST`, `APP_DB_PORT`, `APP_DB_NAME`, `APP_DB_USER`, `APP_DB_PASS` existen.
- Descubre tablas reales de Firebird via `RDB$RELATIONS`.
- El orden declarado de dependencias es razonable para el go-live.

## Riesgos

- Sin riesgos bloqueantes detectados para el alcance corregido. Queda pendiente validar con la FDB final y un runtime Python disponible.

## Pruebas Revisadas

- [x] Revision estatica de `tools/etl_firebird_postgres.py`.
- [x] Comparacion contra esquema PostgreSQL `persona`, `persona_fisica`, `persona_juridica`, `persona_rol` y `activo`.
- [x] Comparacion contra alcance de `REQ-0031`.
- [x] Verificacion de variables `APP_DB_*` en `.env`.
- [x] Build: `mvn -q clean package` en `Desarrollo` con EXIT 0.

## Pruebas Faltantes

- [ ] Dry-run local: no ejecutado porque `python`, `py` y `python3` no estan disponibles/accesibles en esta sesion.
- [ ] Prueba de idempotencia con fixture minimo: dos ejecuciones `--apply` no deben duplicar personas/activos.
- [ ] Prueba de persona juridica/fisica con rol: debe poblar base, especializacion y `persona_rol`.
