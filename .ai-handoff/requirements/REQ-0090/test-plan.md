# REQ-0090 - Plan de pruebas y evidencia

**Fecha:** 2026-07-14

## Pruebas Tecnicas (ejecutadas)

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | Build multi-modulo `mvn clean package` (via deploy-vps.ps1) | compila sin error | Build OK (EXIT 0) |
| T02 | Deploy a la VPS | redeploy exitoso | Redeploy OK, `sginmo-web.war.deployed` presente |
| T03 | App arriba: `curl login.xhtml` | HTTP 200 | HTTP 200 |
| T04 | Render de 37 pantallas: `python tools/smoke-test-vps.py` | todas 200 | === RESULTADO: TODAS OK === |
| T05 | `grep -rnE "\(java\.sql\.Date\) [a-z]" (sin instanceof)` | 0 coincidencias | 0 |

Pantallas del smoke que ejercitan el codigo tocado (todas HTTP 200): `objetivos`, `dashboard-detalle`,
`transferencias`, `cobranza`.

## Pruebas Manuales (portal externo, no automatizable: requiere OTP)

| ID | Escenario | Pasos | Resultado esperado | Resultado real |
|---|---|---|---|---|
| M01 | Ingreso al portal del socio | CI/RUC + OTP + validar | `portal/inicio.xhtml` carga el resumen SIN ClassCastException | pendiente de confirmacion del usuario |
| M02 | Cuotas y pagos del portal | navegar a cuotas/pagos | fechas se muestran correctamente | pendiente de confirmacion del usuario |

## Datos De Prueba

Socio con CI/RUC valido, con email cargado (para recibir el OTP) y con al menos una operacion/cronograma
para que el resumen tenga proxima cuota.
