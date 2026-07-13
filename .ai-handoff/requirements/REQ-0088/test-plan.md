# REQ-0088 - Plan De Pruebas

**Fecha:** 2026-07-13

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | BUILD OK | OK |
| T02 | Deploy VPS + Flyway V54 + smoke | activos 200; 36/36; schema v54 | OK |

## Pruebas Manuales

| ID | Escenario | Pasos | Resultado esperado | Resultado real |
|---|---|---|---|---|
| M01 | Alta con campos nuevos | Nuevo activo, cargar Operacion/Medidas/Anio/Cantidad, Guardar | Guardado sin error | pendiente |
| M02 | Persistencia | Reeditar el activo | Los 4 campos muestran lo cargado | pendiente |
| M03 | Etiqueta | Abrir el form | El contenedor dice "Caracteristicas" | pendiente |

## Datos De Prueba

Una empresa con tipos de activo cargados.
