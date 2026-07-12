# REQ-0050 - Plantillas de documentos: combo de variables disponibles

**Numero:** REQ-0050
**Fecha de creacion:** 2026-07-12
**Estado inicial:** ESPERA_USUARIO
**Prioridad:** media

## Texto Original

En la pantalla de Plantillas de documentos, agregar un combo con la lista de variables disponibles para insertar en el cuerpo.

## Objetivo Funcional

Facilitar la edicion de plantillas permitiendo insertar placeholders validos desde un catalogo cerrado, sin que el usuario deba recordar o tipear manualmente `{{variable}}`.

## Criterios De Aceptacion

- [x] `PlantillaDocumentoBean` expone el catalogo de variables disponibles del motor. (`getVariables()` desde `service.variablesDisponibles()`)
- [x] `plantillas-documentos.xhtml` muestra combo/lista de variables junto al editor del cuerpo. (`h:selectOneMenu id="cboVar"` + boton Insertar, y tab "Variables" con dataTable)
- [x] Al seleccionar una variable y presionar insertar, se agrega el placeholder en la posicion del cursor. (JS `insertarVarCuerpo()` usa `selectionStart/selectionEnd`)
- [x] Si no se puede insertar en el cursor, se agrega al final del cuerpo sin perder texto. (fallback `ta.value.length` cuando `selectionStart` es null; substring preserva el texto)
- [x] Las variables mostradas coinciden con las validadas por el motor. (combo y validacion consumen la misma `PlantillaDocumentoMotor.variablesDisponibles()`)
- [x] No se permiten variables desconocidas al guardar. (validacion existente del motor, sin cambios; el combo solo ofrece variables validas)
- [x] La UI usa textos en espanol y no obliga a escribir placeholders a mano. ("— Elegir variable a insertar —", boton "Insertar"; el placeholder `{{codigo}}` lo arma el JS)

## Dependencias

- Depende de: REQ-0041.
- Requerido por: configuracion amigable de contratos, pagares y documentos.
