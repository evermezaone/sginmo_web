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

---

## Reauditoria - 2026-07-09T08:34:01-04:00

Estado: REQUIERE_CAMBIOS

### Obs 243 - Cerrada parcialmente

El archivo `V26__multiempresa_esquema.sql` ya no esta en `Desarrollo/sginmo-web/src/main/resources/db/migration/`; la carpeta activa de Flyway contiene V1..V25 y V26 vive en `tools/multiempresa/`. `tools/multiempresa/README.md` documenta correctamente que V26 es staging y que solo se promueve junto con F2/F3.

### Obs 244 - El generador sigue escribiendo V26 en el path activo de Flyway

`tools/multiempresa/gen_v26.py:235-238` todavia tiene hardcodeado como salida:

`Desarrollo\sginmo-web\src\main\resources\db\migration\V26__multiempresa_esquema.sql`

Eso contradice la compuerta corregida en `req.md` y `tools/multiempresa/README.md`. Si alguien regenera la evidencia reproducible antes de F2/F3, el script vuelve a crear V26 en el classpath activo de Flyway, reabriendo el riesgo de aplicar la migracion sola en el siguiente deploy.

Correccion requerida: cambiar el generador para escribir por defecto en `tools/multiempresa/V26__multiempresa_esquema.sql` (staging) o exigir un argumento explicito de salida para promocion. La ruta activa `Desarrollo/sginmo-web/src/main/resources/db/migration/V26__multiempresa_esquema.sql` no debe ser el destino por defecto mientras V26 este bloqueada por secuencia.

No se ejecuta `mvn package` porque el REQ vuelve a quedar rechazado por la compuerta incompleta.

---

## Reauditoria - 2026-07-09T08:44:01-04:00

Estado: APROBADO_POR_CODEX

### Obs 243 - Cerrada

Verificado: `Desarrollo/sginmo-web/src/main/resources/db/migration/` contiene V1..V25 y no contiene `V26__multiempresa_esquema.sql`. La migracion V26 vive en `tools/multiempresa/V26__multiempresa_esquema.sql`, fuera del classpath activo de Flyway, con README de promocion manual cuando F2+F3 formen la unidad desplegable.

### Obs 244 - Cerrada

Verificado: `tools/multiempresa/gen_v26.py` ya escribe por defecto junto al propio script mediante `os.path.dirname(os.path.abspath(__file__))`, no en `db/migration`. La promocion a Flyway queda manual/documentada.

## Verificacion final

- `Test-Path Desarrollo/sginmo-web/src/main/resources/db/migration/V26__multiempresa_esquema.sql` -> `False`.
- `Test-Path tools/multiempresa/V26__multiempresa_esquema.sql` -> `True`.
- Build reactor desde `Desarrollo`: `mvn -q clean package` -> EXIT 0.

Sin riesgos bloqueantes restantes para el alcance F1: migracion V26 queda verificada como artefacto staging y no se aplica sola en deploy.
