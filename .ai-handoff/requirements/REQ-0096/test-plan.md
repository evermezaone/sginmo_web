# REQ-0096 - Plan de pruebas

**Fecha:** 2026-07-14

## Pruebas Tecnicas (ejecutadas)

| ID | Escenario | Esperado | Real |
|---|---|---|---|
| T01 | Build mvn clean package | compila | Build OK (EXIT 0) |
| T02 | Deploy VPS | redeploy | Redeploy OK |
| T03 | Render 37 pantallas (smoke) | 200 | === RESULTADO: TODAS OK === |

## Pruebas Manuales (portal, requiere OTP)

| ID | Escenario | Pasos | Esperado | Real |
|---|---|---|---|---|
| M01 | Portal en celular | abrir inicio en ~360px | sin scroll horizontal de pagina; tarjetas 2x2; tablas scrollean dentro de la tarjeta; header ordenado | pendiente de confirmacion del usuario |
| M02 | Transferencia en celular | abrir transferencia.xhtml en ~360px | formulario y tabla se ven bien, sin desborde | pendiente de confirmacion del usuario |

## Datos De Prueba

Socio con cuotas/pagos para poblar las tablas.
