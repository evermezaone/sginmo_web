# REQ-0048 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package -DskipTests` | Build OK | OK |
| T02 | V31 en prod (Flyway) | migra tipo de columna y siembra CLASIFICACION_ARTICULO | OK (`success=t` en flyway_schema_history) |
| T03 | `python tools/smoke-test-vps.py` | 19/19 RENDER OK incl. `articulos` | OK (RESULTADO: TODAS OK) |
| T04 | `articulos.xhtml` usa combo | el campo Clasificacion es `p:selectOneMenu` sobre `articuloBean.clasificaciones` | OK (verificado en marcado) |

## Pruebas Manuales

| ID | Escenario | Pasos | Resultado esperado | Resultado real |
|---|---|---|---|---|
| M01 | Ver combo | ABM Articulos -> pestana Clasificacion | Lista desplegable con General/Servicio/Gasto/Otro | pendiente (verificacion visual del usuario) |
| M02 | Persistir seleccion | Guardar articulo con clasificacion y reabrir | Conserva la opcion elegida | pendiente (verificacion visual del usuario) |

## Datos De Prueba

Semillas globales sembradas por V31 (tenant -1): General, Servicio, Gasto, Otro.
