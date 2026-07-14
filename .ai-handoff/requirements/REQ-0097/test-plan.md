# REQ-0097 - Plan de pruebas

**Fecha:** 2026-07-14

## Tecnicas (ejecutadas)
| ID | Escenario | Esperado | Real |
|---|---|---|---|
| T01 | inicio.xhtml bien formado | OK | OK |
| T02 | Build mvn clean package | compila | Build OK |
| T03 | Deploy VPS | redeploy | Redeploy OK |
| T04 | Smoke 37 pantallas | 200 | TODAS OK |

## Manuales (portal, requiere OTP)
| ID | Escenario | Esperado | Real |
|---|---|---|---|
| M01 | Cuota vencida impaga | Atrasado + dias>0 + multa (f_mora_cuota) | pendiente |
| M02 | Cuota futura impaga | Al dia + 0 | pendiente |
| M03 | Cuota pagada | Pagado, sin mora | pendiente |
| M04 | Consistencia | multa igual a cobranza para misma cuota/fecha | pendiente |

## Datos De Prueba
Socio con cuotas en distintos estados (vencida impaga, futura, pagada).
