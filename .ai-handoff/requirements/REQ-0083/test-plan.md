# REQ-0083 (Fase 1) - Plan De Pruebas

**Fecha:** 2026-07-13

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | BUILD OK | OK |
| T02 | Deploy VPS + Flyway V56 + smoke | transferencias 200; 37/37; schema v56 | OK |
| T03 | Portal protegido | GET /portal/transferencia sin sesion -> 302 login | OK |

## Pruebas Manuales

| ID | Escenario | Pasos | Resultado esperado | Resultado real |
|---|---|---|---|---|
| M01 | Informar | Socio en portal -> Informar transferencia -> importe+comprobante -> enviar | Queda RECIBIDO; aparece en "Mis transferencias" | pendiente |
| M02 | Validacion archivo | Adjuntar un .txt o archivo grande | Rechaza (formato/tamano) | pendiente |
| M03 | Bandeja aprobar | Interno abre Transferencias -> Revisar -> elegir documento+emisor -> Aprobar (con caja abierta) | Genera cobro, imputa, estado APLICADO | pendiente |
| M04 | Observar/Rechazar | En la bandeja, con motivo | Estado OBSERVADO/RECHAZADO; el socio ve el motivo | pendiente |
| M05 | Anti-doble | Intentar aplicar dos veces el mismo nro | Rechaza (unique parcial) | pendiente |

## Datos De Prueba

Un socio con email (portal accesible) + una caja abierta + un documento pendiente del cliente.
