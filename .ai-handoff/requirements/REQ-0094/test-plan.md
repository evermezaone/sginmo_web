# REQ-0094 - Plan de pruebas

**Fecha:** 2026-07-14

## Pruebas Tecnicas (ejecutadas)

| ID | Escenario | Esperado | Real |
|---|---|---|---|
| T01 | XHTML bien formados | parse OK | OK |
| T02 | Build | compila | Build OK (EXIT 0) |
| T03 | Migracion V60 | crea portal_pago_qr | Redeploy OK (migrate corrio) |
| T04 | Deploy VPS | redeploy | Redeploy OK |
| T05 | Render 37 pantallas admin (smoke) | 200 (incluye transferencias con panel QR) | TODAS OK |

## Pruebas Manuales (requiere OTP + QR habilitado)

| ID | Escenario | Pasos | Esperado | Real |
|---|---|---|---|---|
| M01 | QR dinamico | entrar al portal con QR habilitado | se crea intencion con referencia unica; el QR la lleva | pendiente confirmacion |
| M02 | Auto-match | registrar/importar movimiento con esa referencia + importe | intento -> CONCILIADO; aparece en el panel de la bandeja | pendiente confirmacion |

## Datos De Prueba

QR habilitado (REQ-0093); un socio con saldo; acceso a la bandeja de transferencias para cargar el movimiento.
