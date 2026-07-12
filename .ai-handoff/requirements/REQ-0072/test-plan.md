# REQ-0072 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | Build OK | OK |
| T02 | Flyway V48 | param OCUPACION_OBJETIVO_PCT + pantalla ocupacion | OK (log: "Migrating to version 48 - ocupacion") |
| T03 | Deploy + smoke | 32/32 incl. ocupacion | OK (tras corregir panelGrid columns=5 -> flex) |
| T04 | Regla alquilable | excluye VENDIDA y sin precio_alquiler | OK (por codigo/SQL) |
| T05 | Brecha | objetivoUnidades=ceil(obj%*alq); brecha=max(0,obj-ocupados) | OK (por codigo) |

## Pruebas Manuales

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | Datos reales con alquileres vigentes | ocupacion/vacantes/brecha coherentes | pendiente (verificacion del usuario) |
| M02 | Cambiar OCUPACION_OBJETIVO_PCT | la brecha cambia | pendiente |
| M03 | Lista de vacantes | ordenada por precio_alquiler DESC; primeras `brecha` marcadas | pendiente |
| M04 | Dos empresas | cada una ve solo sus activos (RLS) | pendiente |

## Datos De Prueba

Activos con precio_alquiler>0, algunos con operacion de alquiler vigente y otros vacantes.

## Nota

La pantalla ocupacion sirve tambien como evidencia (drill-down) de vacancia para REQ-0074.
