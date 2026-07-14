# REQ-0093 - Plan de pruebas

**Fecha:** 2026-07-14

## Pruebas Tecnicas (ejecutadas)

| ID | Escenario | Esperado | Real |
|---|---|---|---|
| T01 | XHTML bien formados | parse OK | OK |
| T02 | Build con ZXing | compila | Build OK (EXIT 0) |
| T03 | Migracion V59 | aplica | Redeploy OK (migrate corrio) |
| T04 | Deploy VPS | redeploy | Redeploy OK |
| T05 | Render 37 pantallas admin (smoke) | 200 | TODAS OK |

## Pruebas Manuales (portal, requiere OTP + parametros)

| ID | Escenario | Pasos | Esperado | Real |
|---|---|---|---|---|
| M01 | QR deshabilitado | sin PORTAL_QR_CUENTA | no aparece la seccion "Pagar por QR" | pendiente confirmacion |
| M02 | QR habilitado | cargar cuenta + habilitar | aparece el QR con el monto adeudado | pendiente confirmacion |
| M03 | Escaneo | app bancaria EMVCo/SIPAP | lee el QR (validar tag 26 con el banco) | pendiente confirmacion |

## Datos De Prueba

Parametros PORTAL_QR_CUENTA/GUI/MERCHANT + PORTAL_QR_HABILITADO=true; socio con deuda vencida o proxima cuota.
