# REQ-0050 - Codex Review

## Decision

APROBADO_POR_CODEX.

## Revision

- `PlantillaDocumentoMotor.variablesDisponibles()` define el catalogo cerrado de variables.
- `PlantillaDocumentoService.variablesDisponibles()` expone el mismo catalogo al bean.
- `plantillas-documentos.xhtml` muestra selector de variables y boton Insertar.
- El JavaScript inserta `{{variable}}` en la posicion del cursor, o al final cuando el cursor no esta disponible.
- La validacion existente del motor sigue rechazando variables desconocidas al guardar.

## Verificacion

- `mvn -q clean package`: OK.
