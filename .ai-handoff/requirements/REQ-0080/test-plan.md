# REQ-0080 - Plan De Pruebas

**Fecha:** 2026-07-13

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | BUILD OK | OK |
| T02 | Smoke render con cobros existentes | comprobantes 200 (antes 500); 36/36 | OK |

## Pruebas Manuales

| ID | Escenario | Pasos | Resultado esperado | Resultado real |
|---|---|---|---|---|
| M01 | Listado de comprobantes | Registrar un cobro y abrir comprobantes | Lista sin error, con la fecha correcta | pendiente |
| M02 | Reimpresion de recibo | Reimprimir recibo de un cobro | PDF con fecha correcta | pendiente |

## Datos De Prueba

Al menos un cobro registrado (columna date presente).
