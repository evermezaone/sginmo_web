# REQ-0076 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | Build OK | OK |
| T02 | Deploy + smoke | 36/36 (sin regresion) | OK |
| T03 | Columnas rango_comprobante | tenant/tipo/serie/numero_*/estado (V26 rename) + version DEFAULT 0 | OK (por migraciones) |
| T04 | ErroresBd surface P0001 | RAISE -> NegocioException con mensaje limpio | OK (por codigo) |

## Pruebas Manuales

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | Alta de operacion a credito | se registra + cronograma; autoprovision DINT si faltaba | pendiente (verificacion del usuario) |
| M02 | Error del motor (RAISE) | mensaje visible en el dialogo, no falla silenciosa | pendiente |
| M03 | Reintento tras error | mensaje claro, no "Detached entity" silencioso | pendiente |

## Datos De Prueba

Una empresa con al menos una sucursal y un activo LIBRE.

## Nota

El documento interno DINT/OP no es fiscal; se autoprovisiona un numerador interno amplio por tenant.
