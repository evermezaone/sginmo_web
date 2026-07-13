# REQ-0077 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | Build OK | OK |
| T02 | Deploy + smoke | 36/36 (sin regresion) | OK |
| T03 | txtMotivoResc sin required | el submit completo de los botones ajax=false no valida el motivo | OK (por codigo) |

## Pruebas Manuales

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | Detalle -> Estado de cuenta/Contrato/Pagares | descarga el PDF, sin mensaje del motivo | pendiente (verificacion del usuario) |
| M02 | Finalizar sin motivo | "El motivo de la finalizacion/rescision es obligatorio" (server-side) | pendiente |
| M03 | Finalizar con motivo | finaliza y libera el activo | pendiente |
| M04 | Renovar / regenerar cuotas | funcionan | pendiente |

## Datos De Prueba

Una operacion de alquiler a credito con cronograma.
