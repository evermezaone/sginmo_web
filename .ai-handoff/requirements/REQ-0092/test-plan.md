# REQ-0092 - Plan de pruebas

**Fecha:** 2026-07-14

## Pruebas Tecnicas (ejecutadas)

| ID | Escenario | Esperado | Real |
|---|---|---|---|
| T01 | transferencia.xhtml / inicio.xhtml bien formados | parse OK | OK |
| T02 | Build mvn clean package | compila | Build OK (EXIT 0) |
| T03 | Deploy VPS | redeploy | Redeploy OK |
| T04 | Render 37 pantallas admin (smoke) | 200 | TODAS OK |

## Pruebas Manuales (portal, requiere OTP)

| ID | Escenario | Pasos | Esperado | Real |
|---|---|---|---|---|
| M01 | Estado y borrado | informar transferencia | figura "Pendiente de validacion" con Descargar + Eliminar | pendiente confirmacion usuario |
| M02 | Borrado en pendiente | click Eliminar | se borra el registro y la evidencia | pendiente confirmacion usuario |
| M03 | Bloqueo en verificacion | operador toma la transferencia | el socio ya no puede eliminar (boton oculto; backend rechaza) | pendiente confirmacion usuario |

## Datos De Prueba

Socio con sesion de portal; un operador con acceso a la bandeja de transferencias.
