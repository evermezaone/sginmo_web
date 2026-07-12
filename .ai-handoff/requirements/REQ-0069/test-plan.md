# REQ-0069 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | Build OK (EXIT 0) | OK |
| T02 | Deploy + redeploy | login HTTP 200 | OK |
| T03 | `python tools/smoke-test-vps.py` | 31/31 (backend, sin regresion) | OK |
| T04 | Periodos.para(mes en curso) | actual=[1,hoy]; anterior y AA con mismos dias transcurridos | OK (por codigo) |
| T05 | Periodos.para(mes cerrado) | actual/anterior/AA = meses completos | OK (por codigo) |
| T06 | Variacion con base 0 | porcentual=NA, direccion segun signo | OK (por codigo) |

## Pruebas Manuales (se completan con REQ-0070)

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | comparativos() con datos reales | mom/yoy/ytd coherentes por indicador | pendiente (UI en 0070) |
| M02 | KPI monetario sin moneda | aplicable=false (no mezcla monedas) | pendiente |
| M03 | Dos empresas | cada una ve solo sus numeros (RLS) | pendiente |

## Datos De Prueba

Cobros/operaciones/ingreso_egreso de al menos 2 meses para ver comparativos.

## Nota De Alcance

REQ-0069 es el motor (sin UI propia); la verificacion visual/end-to-end llega con REQ-0070, que lo consume.
