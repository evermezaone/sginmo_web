# Implementacion Claude - REQ-0033

## Manifiesto Minimo Para Codex
Migracion `V26__multiempresa_esquema.sql` que transforma el esquema V25 al modelo
multiempresa (doc 14 v2.2). Construida a partir del `pg_dump --schema-only` real de la
VPS + `information_schema` (nombres y nullabilidad exactos), no de memoria. Puntos que
merecen la mirada del auditor:

- **entidad**: PK vieja `(entidad,codigo)` -> PK numerica `entidad` (IDENTITY, rellena
  las 108 filas existentes) + `UNIQUE(lista,codigo,tenant)`. Las 29 FK compuestas se
  sueltan dinamicamente (DO block que recorre `pg_constraint WHERE confrelid=entidad`).
- **Mapeo por id sin perdida**: cada `UPDATE t SET ref = e.entidad FROM entidad e WHERE
  e.lista=t.ref_lista AND e.codigo=t.ref_codigo`. Las FK compuestas viejas garantizaban
  integridad referencial, asi que todo par no-nulo mapea (verificado: 8276 ubicaciones,
  0 nulos; SET NOT NULL exito donde correspondia).
- **Vistas**: `v_persona` depende de columnas que se dropean/mueven -> se suelta al inicio
  y se recrea al final (tenant-aware sobre `persona_empresa`). `v_operacion_saldo` gana `tenant`.
- **Clasificacion -1 vs tenant**: semillas de sistema (monedas, impuestos, arbol INE,
  formas de pago base, articulos, grupos plantilla, parametros default, rol EMPRESA) van
  a `-1` (global bloqueado). Los datos de Pysistemas (persona_empresa, persona_rol
  PROVEEDOR, sucursal) quedan en tenant 1.
- **SUPERADMIN**: se crea reutilizando el `password_hash` del usuario `admin` (misma
  clave, cambiable), tenant -1.

**Archivos:** `Desarrollo/sginmo-web/src/main/resources/db/migration/V26__multiempresa_esquema.sql`;
`tools/multiempresa/gen_v26.py` (generador reproducible); `tools/multiempresa/v26_checks.sql` (bateria de asserts).

**Comandos probados:** batería `BEGIN; <V26>; <checks>; ROLLBACK;` corrida via
`psql -v ON_ERROR_STOP=1` contra la BD real de la VPS -> **EXIT=0**, todos los asserts
en verde y sin persistir nada. Evidencia en `test-plan.md`.

## Nota de secuencia (importante para el auditor)
V26 NO se aplica aun a la BD viva (sigue en V25). Aplicarla sola romperia la app
desplegada (entidades/SP referencian empresa/tipo_codigo/*_lista). Se despliega junto a
F2 (REQ-0034) y F3 (REQ-0035). Esta revision es del diseño y correctitud de la migracion.

## obs 243 — compuerta de despliegue (RESUELTA)
El bloqueo de secuencia ahora es EFECTIVO, no solo documental: V26 se movio a
`tools/multiempresa/V26__multiempresa_esquema.sql` (FUERA de `classpath:db/migration`
que `FlywayMigrator` ejecuta al arranque). Asi ningun deploy puede aplicar V26 antes de
F2/F3. `db/migration/` queda con V1..V25. V26 se promueve a esa carpeta (git mv, ver
`tools/multiempresa/README.md`) recien cuando F2+F3 esten listos como unidad desplegable.
La evidencia reproducible (gen_v26.py + v26_checks.sql + rollback battery) sigue en staging.

## obs 244 — el generador ya no escribe en el path de Flyway (RESUELTA)
`gen_v26.py` escribia por defecto en `Desarrollo/sginmo-web/src/main/resources/db/migration/`,
asi que regenerar la evidencia re-plantaba V26 en el classpath activo de Flyway. Corregido: ahora
escribe junto al propio script (`os.path.dirname(__file__)` -> `tools/multiempresa/V26__*.sql`), con
comentario que remite a la compuerta. Ademas se sincronizo el generador con el V26 real (le faltaban
el DROP/recreate de vistas): regenerar produce un archivo IDENTICO al staged (diff vacio) y la bateria
BEGIN..ROLLBACK sobre el regenerado da EXIT=0 con todos los asserts en verde. Reproducibilidad real.
