# REQ-0074 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | Build OK | OK |
| T02 | Deploy + smoke | 35/35 incl. dashboard-detalle | OK (tras cambiar p:message sin for por div) |
| T03 | Clave no whitelisteada | NegocioException (no ejecuta query) | OK (por codigo) |
| T04 | Permiso del modulo origen | exige (pantalla,accion) segun clave | OK (por codigo) |
| T05 | Filtros tipados | LocalDate/Long; sin SQL libre | OK (por codigo) |

## Pruebas Manuales

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | clave=mora&hasta&moneda | cuotas vencidas de esa moneda | pendiente (verificacion del usuario) |
| M02 | clave=vacancia | propiedades vacantes + export CSV | pendiente |
| M03 | Usuario sin permiso origen | detalle no visible | pendiente |
| M04 | Dos empresas | RLS aisla | pendiente |

## Datos De Prueba

Cobros/cuotas/ingreso_egreso/activos del periodo.

## Nota

Los drillKey de 0069/0071/0072/0073 mapean a estas claves; el dashboard visual (0070) los enlazara.
