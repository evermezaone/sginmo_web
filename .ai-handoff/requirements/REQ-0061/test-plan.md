# REQ-0061 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | Build OK | OK |
| T02 | V42 en `BEGIN...ROLLBACK` | tabla + 4 RLS + pantalla | OK |
| T03 | Backup previo | dump OK | OK |
| T04 | Deploy + Flyway V42 | success=t | OK |
| T05 | `python tools/smoke-test-vps.py` | 27/27 render OK incl. importacion | OK (TODAS OK) |

## Pruebas Manuales

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | CSV PARAMETRO con 1 fila invalida | preview marca la fila ERROR; confirmar no inserta (atomico) | pendiente |
| M02 | CSV PARAMETRO todo valido | confirmar inserta; historial OK | pendiente |
| M03 | Descargar plantilla | CSV con encabezado del tipo | pendiente |

## Revision Transversal

- Atomicidad: importar valida todas las filas; con ≥1 error no inserta ninguna; historial registra el intento.
- Reuso de validaciones: PARAMETRO -> ParametroService.guardar (unicidad/tenant); no se duplican reglas.
- Aislamiento: tabla importacion con RLS por tenant; el importador inserta para el tenant actual.
- "No reemplaza ETL Firebird": documentado; es herramienta operativa.

## Datos De Prueba

Un CSV UTF-8 con encabezado `clave,valor,descripcion,grupo,tipo` y 2-3 filas.
