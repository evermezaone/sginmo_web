# REQ-0050 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0050
- Tipo de cambio: UI + backend liviano (combo de variables + JS de insercion)
- Riesgo: bajo (no cambia el motor de plantillas ni su validacion)
- Archivos clave:
  - `sginmo-web/src/main/webapp/plantillas-documentos.xhtml`: `h:selectOneMenu id="cboVar"` sobre `plantillaDocumentoBean.variables`, boton "Insertar" y JS `insertarVarCuerpo()` que inserta `{{codigo}}` en la posicion del cursor del textarea del cuerpo. Tab "Variables" con `dataTable` de referencia.
  - `sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/PlantillaDocumentoBean.java`: campo `varSeleccionada` (enlace del `<select>`) y `getVariables()` desde `service.variablesDisponibles()`.
- Comandos probados:
  - `mvn -q clean package -DskipTests`: BUILD OK.
  - Deploy a la VPS (WAR + `.dodeploy`): OK.
  - `python tools/smoke-test-vps.py`: 19/19 pantallas RENDER OK (incluida `plantillas-documentos`).
- Cambios de datos: no.
- Cambios de entorno: no.
- Impacto LLM/tokens: no.
- Decision esperada: aprobar (UI de bajo riesgo).
- Notas para auditor:
  - El insert lo hace JS en el cliente (`selectionStart/selectionEnd`); `varSeleccionada` es solo enlace del combo, no persiste.
  - La validacion de variables desconocidas al guardar es la existente del motor (`PlantillaDocumentoMotor`), sin cambios; el combo solo ofrece variables validas de `variablesDisponibles()`.
  - Fallback: si el navegador no expone `selectionStart`, se inserta al final del cuerpo sin perder texto.

## Resumen Funcional

En Plantillas de documentos, el usuario elige una variable de un combo y presiona "Insertar" para
agregar el placeholder `{{variable}}` en la posicion del cursor del cuerpo, sin tener que recordarla
ni tipearla a mano. Ademas hay un tab "Variables" que lista todas las disponibles.

## Resumen Tecnico

`plantillas-documentos.xhtml` agrega un `h:selectOneMenu` (`cboVar`) enlazado a
`plantillaDocumentoBean.varSeleccionada`, poblado con `getVariables()` (motor). Un boton `Insertar`
llama a JS `insertarVarCuerpo()` que toma `selectionStart/selectionEnd` del textarea del cuerpo e
inserta `{{codigo}}`; si no hay seleccion disponible, agrega al final. El bean solo expone las
variables y guarda el valor elegido del combo; la validacion de placeholders al guardar es la del
motor y no se toco.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| webapp/plantillas-documentos.xhtml | combo de variables + boton Insertar + JS `insertarVarCuerpo` + tab "Variables" |
| web/PlantillaDocumentoBean.java | campo `varSeleccionada` (enlace del combo) |

## Cambios De Datos

Sin cambios.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Ver `test-plan.md`. Resumen: build OK, deploy OK, smoke-test 19/19 RENDER OK.

## Pruebas Manuales Sugeridas

1. Editar una plantilla, colocar el cursor en medio del cuerpo, elegir una variable e insertar: el `{{codigo}}` debe quedar en la posicion del cursor.
2. Insertar con el cursor fuera del textarea: el placeholder debe agregarse al final sin borrar texto.

## Riesgos Conocidos

- La insercion depende de JS del cliente; con JS deshabilitado el combo no inserta (el usuario aun puede tipear la variable).
