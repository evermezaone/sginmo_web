# REQ-0050 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package -DskipTests` | Build OK | OK |
| T02 | Deploy a la VPS (WAR + `.dodeploy`) | despliegue sin error | OK |
| T03 | `python tools/smoke-test-vps.py` | 19/19 RENDER OK incl. `plantillas-documentos` | OK (RESULTADO: TODAS OK) |
| T04 | Marcado del combo | `h:selectOneMenu id="cboVar"` + boton Insertar + JS `insertarVarCuerpo()` sobre `variables` del bean | OK (verificado en marcado) |

## Pruebas Manuales

| ID | Escenario | Pasos | Resultado esperado | Resultado real |
|---|---|---|---|---|
| M01 | Insertar en cursor | Editar plantilla, cursor en medio del cuerpo, elegir variable e Insertar | `{{codigo}}` queda en la posicion del cursor | pendiente (verificacion visual del usuario) |
| M02 | Fallback sin seleccion | Insertar sin foco en el textarea | Placeholder al final sin perder texto | pendiente (verificacion visual del usuario) |

## Datos De Prueba

Variables provistas por `PlantillaDocumentoMotor.variablesDisponibles()` (catalogo del motor).
