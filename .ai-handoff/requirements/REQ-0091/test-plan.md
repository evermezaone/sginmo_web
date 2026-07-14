# REQ-0091 - Plan de pruebas

**Fecha:** 2026-07-14

## Pruebas Tecnicas (ejecutadas)

| ID | Escenario | Esperado | Real |
|---|---|---|---|
| T01 | inicio.xhtml bien formado | parse XML OK | OK |
| T02 | Build mvn clean package | compila | Build OK (EXIT 0) |
| T03 | Deploy VPS | redeploy | Redeploy OK |
| T04 | Render 37 pantallas admin (smoke) | 200 | === RESULTADO: TODAS OK === |

## Pruebas Manuales (portal, requiere OTP)

| ID | Escenario | Pasos | Esperado | Real |
|---|---|---|---|---|
| M01 | Panel de pagos | login socio | panel "Mis pagos" a la derecha con badge Caja/Transferencia por pago | pendiente confirmacion usuario |
| M02 | Responsive | ~360px | el panel de pagos baja debajo del contenido | pendiente confirmacion usuario |

## Datos De Prueba

Socio con al menos un cobro por caja y uno por transferencia (forma_pago TRF) para ver ambos badges.
