# REQ-0102 - Plan de pruebas
**Fecha:** 2026-07-16
## Tecnicas (ejecutadas)
| ID | Escenario | Esperado | Real |
|---|---|---|---|
| T01 | XML bien formados | OK | OK |
| T02 | Build/deploy | OK | Build OK / Redeploy OK |
| T03 | Smoke 37 | 200 | TODAS OK |
## Manuales (portal, requiere credenciales)
| ID | Escenario | Esperado | Real |
|---|---|---|---|
| M01 | Login sin lista de empresas | solo documento+password | pendiente |
| M02 | Documento de 1 empresa | entra transparente, sin selector | pendiente |
| M03 | Documento de N empresas | selector arriba; cambia de empresa | pendiente |
| M04 | Primer ingreso por documento | OTP + clave; sirve en todas sus empresas | pendiente |
| M05 | Aislamiento | no se accede a empresa cuya clave no abre | pendiente |
## Datos De Prueba
Un documento cliente de 1 empresa y otro de 2+ empresas.
