# REQ-0071 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | Build OK | OK |
| T02 | Flyway V49 | pantalla rentabilidad | OK |
| T03 | Deploy + smoke | 33/33 incl. rentabilidad | OK |
| T04 | Clasificacion por aplicacion | GROUP BY articulo.aplicacion | OK (por SQL) |
| T05 | Deposito excluido del neto | totalDepositos separado; neto = ingresos operativos - egresos | OK (por codigo) |

## Pruebas Manuales

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | ingreso_egreso real del periodo | ingresos/egresos por tipo + neto + margen | pendiente (verificacion del usuario) |
| M02 | Ranking de activos | ordenado por neto (mejores/peores) | pendiente |
| M03 | Dos empresas | cada una ve solo lo suyo (RLS) | pendiente |

## Datos De Prueba

Movimientos ingreso_egreso (CANCELADO) de varios tipos y activos en el periodo.

## Nota

Los drillKey (`ingreso_egreso:TIPO:APLICACION`, `rentabilidad_activo:ID`) alimentan el detalle de REQ-0074.
