# REQ-0087 - Plan De Pruebas

**Fecha:** 2026-07-13

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | BUILD OK | OK |
| T02 | Deploy VPS + Flyway V55 + smoke | activos 200; 36/36; schema v55 | OK |

## Pruebas Manuales

| ID | Escenario | Pasos | Resultado esperado | Resultado real |
|---|---|---|---|---|
| M01 | GENERAR=LOTES | Nuevo, GENERAR=LOTES, loteamiento+manzana+cantidad, Generar | Se crean lotes | pendiente |
| M02 | GENERAR=CASAS/DPTOS | Nuevo, GENERAR=CASAS/DPTOS, Tipo+cochera+m2+medida+ANDE+ESSAP, Guardar | Guardado; reeditar muestra los campos | pendiente |
| M03 | Editar lote | Editar un lote, ver Datos del lote (superficie/dimensiones), Guardar | Persisten; tipo fijo Lote | pendiente |
| M04 | Tipo filtrado | En casa/dpto abrir el combo Tipo | No aparecen Lote ni Loteamiento | pendiente |

## Datos De Prueba

Un loteamiento (activo tipo Loteamiento) y algun lote existente para editar.
