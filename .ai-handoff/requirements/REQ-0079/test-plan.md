# REQ-0079 - Plan De Pruebas

**Fecha:** 2026-07-13

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | BUILD OK | OK |
| T02 | Deploy VPS + smoke render | caja 200; 36/36 | OK |

## Pruebas Manuales

| ID | Escenario | Pasos | Resultado esperado | Resultado real |
|---|---|---|---|---|
| M01 | Grilla por defecto | Abrir caja con cobros | No hay X; solo boton "Anular ultimo cobro..." y recibo PDF | pendiente |
| M02 | Habilitar modo | Click en "Anular ultimo cobro..." | Aviso rojo + selector de motivo; la X aparece solo en el ultimo cobro de hoy | pendiente |
| M03 | Anular sin motivo | Modo activo, X, sin elegir motivo | Rechaza ("Elija el motivo") | pendiente |
| M04 | Anular con motivo | Modo activo, elegir motivo, X, confirmar | Anula, repone saldo/cuotas, apaga el modo, auditado | pendiente |
| M05 | Backend anti-bypass | Forzar anular de un cobro no-ultimo o de dia anterior | NegocioException ("solo el ultimo cobro" / "solo un cobro de hoy") | pendiente |

## Datos De Prueba

Caja abierta con >= 2 cobros del dia y algun cobro de dia anterior.
