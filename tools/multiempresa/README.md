# Multiempresa — migración V26 en staging (fuera de Flyway)

`V26__multiempresa_esquema.sql` vive **acá a propósito**, NO en
`Desarrollo/sginmo-web/src/main/resources/db/migration/`.

**Por qué (obs 243):** `FlywayMigrator` corre `flyway.migrate()` sobre
`classpath:db/migration` en cada arranque. Si V26 estuviera en esa carpeta, el
próximo despliegue la aplicaría SOLA — y V26 rompe la app desplegada, porque las
entidades JPA y los SP todavía referencian `empresa`, `tipo_codigo` y los pares
`*_lista/*_codigo`. El propio REQ-0033 declara que V26 NO debe aplicarse sola.

**Compuerta de despliegue:** V26 se PROMUEVE a `db/migration/` (para que Flyway la
tome) **únicamente** cuando F2 (REQ-0034, entidades) y F3 (REQ-0035, SP + SQL nativo)
estén completos y formen la misma unidad desplegable. Recién ahí el deploy aplica
V26 + V27(F3) juntos y la app arranca coherente.

Comando de promoción (cuando F2+F3 estén listos):

    git mv tools/multiempresa/V26__multiempresa_esquema.sql \
           Desarrollo/sginmo-web/src/main/resources/db/migration/V26__multiempresa_esquema.sql

**Evidencia reproducible (se mantiene acá):**
- `gen_v26.py` — genera V26 desde el esquema real.
- `v26_checks.sql` — batería de asserts.
- Verificación: `BEGIN; <V26>; <v26_checks.sql>; ROLLBACK;` via `psql -v ON_ERROR_STOP=1`
  contra la BD real de la VPS → EXIT=0, sin persistir (la BD viva sigue en V25).
