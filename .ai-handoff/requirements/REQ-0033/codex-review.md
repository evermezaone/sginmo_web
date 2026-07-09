# Codex Review - REQ-0033

Estado: REQUIERE_CAMBIOS
Fecha: 2026-07-09T08:14:01-04:00
Auditor: codex

## Observaciones bloqueantes

### Obs 243 - V26 queda empaquetada como migracion Flyway activa aunque el REQ exige no aplicarla sola

`V26__multiempresa_esquema.sql` fue entregada dentro de `Desarrollo/sginmo-web/src/main/resources/db/migration/`, que es la ruta activa que `FlywayMigrator` ejecuta en cada arranque:

- `FlywayMigrator.java:36-43` configura `locations("classpath:db/migration")`, `baselineVersion("21")` y ejecuta `flyway.migrate()`.
- `REQ-0033/req.md:38-41` documenta que V26 NO debe aplicarse aun a la BD viva porque rompe la app desplegada y debe ir junto con F2/F3.
- `REQ-0033/claude-implementation.md:32-35` repite que aplicar V26 sola rompe entidades/SP actuales.

Con la migracion en esa carpeta, el proximo despliegue de la app puede aplicar V26 automaticamente antes de F2/F3. Eso contradice el bloqueo formal del REQ y deja el release en riesgo: la BD pasaria a esquema multiempresa mientras la aplicacion y los SP siguen referenciando `empresa`, `tipo_codigo` y pares `*_lista/*_codigo`.

Correccion requerida: retirar V26 del path activo de Flyway hasta que F2/F3 formen la misma unidad desplegable, o agregar una compuerta explicita de despliegue que impida ejecutar V26 en entornos vivos hasta que el paquete completo este listo. La solucion debe mantener evidencia reproducible de la migracion, pero no puede quedar como `V26__*.sql` ejecutable en `classpath:db/migration` si el release actual no debe aplicarla.

## Verificacion realizada

- Leido `req.md`, `claude-implementation.md` y `test-plan.md`.
- Verificada existencia de `Desarrollo/sginmo-web/src/main/resources/db/migration/V26__multiempresa_esquema.sql`.
- Verificada configuracion real de Flyway en `FlywayMigrator.java`.

No se ejecuta `mvn package` como criterio de aprobacion porque el REQ queda rechazado por bloqueo de secuencia/despliegue.
