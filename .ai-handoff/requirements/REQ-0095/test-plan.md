# REQ-0095 - Plan de pruebas

**Fecha:** 2026-07-14

## Pruebas Tecnicas (ejecutadas)

| ID | Escenario | Esperado | Real |
|---|---|---|---|
| T01 | Build mvn clean package | compila | Build OK (EXIT 0) |
| T02 | Deploy VPS | redeploy | Redeploy OK |
| T03 | Render 37 pantallas (smoke) | 200 | === RESULTADO: TODAS OK === |

## Pruebas Manuales (portal externo, requiere OTP)

| ID | Escenario | Pasos | Esperado | Real |
|---|---|---|---|---|
| M01 | Bienvenida en inicio | login socio (CI/RUC+OTP) | se ve "Bienvenido, {nombre}" grande y legible | pendiente de confirmacion del usuario |

## Datos De Prueba

Socio valido con nombre cargado en la persona.
