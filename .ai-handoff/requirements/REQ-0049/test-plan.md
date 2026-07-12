# REQ-0049 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package -DskipTests` | Build OK | OK |
| T02 | Deploy a la VPS (WAR + `.dodeploy`) | despliegue sin error | OK |
| T03 | `python tools/smoke-test-vps.py` | 19/19 RENDER OK incl. `operaciones` | OK (RESULTADO: TODAS OK) |
| T04 | Marcado del dialogo | cuerpo con `overflow-y:auto` + `div.pie-dialogo` con botones fuera del scroll | OK (verificado en marcado) |

## Pruebas Manuales

| ID | Escenario | Pasos | Resultado esperado | Resultado real |
|---|---|---|---|---|
| M01 | Botones visibles | Abrir alta de Operacion en pantalla chica | Cancelar/Registrar visibles sin scrollear la pagina | pendiente (verificacion visual del usuario) |
| M02 | Submit intacto | Cargar y registrar una operacion | Validacion y submit funcionan igual que antes | pendiente (verificacion visual del usuario) |

## Datos De Prueba

Ninguno especial.
