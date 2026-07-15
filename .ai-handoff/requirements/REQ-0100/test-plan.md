# REQ-0100 - Plan de pruebas

**Fecha:** 2026-07-14

## Tecnicas (ejecutadas)
| ID | Escenario | Esperado | Real |
|---|---|---|---|
| T01 | XHTML bien formados | OK | OK |
| T02 | Build | compila | Build OK |
| T03 | Deploy VPS | redeploy | Redeploy OK |
| T04 | Smoke 37 pantallas | 200 | TODAS OK |

## Manuales (portal, requiere OTP)
| ID | Escenario | Esperado | Real |
|---|---|---|---|
| M01 | En proceso | informar transferencia | aparece en "Transferencias en proceso" (inicio) con fecha/monto/estado | pendiente |
| M02 | Aplicada | aprobar/auto-apply | sale de "en proceso" y figura en Mis pagos | pendiente |
| M03 | Volver | boton en transferencia.xhtml | vuelve a inicio | pendiente |

## Datos De Prueba
Socio con al menos una transferencia informada no aplicada.
