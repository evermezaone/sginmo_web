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

- [ ] `PlantillaDocumentoBean` expone el catalogo de variables disponibles del motor.
- [ ] `plantillas-documentos.xhtml` muestra combo/lista de variables junto al editor del cuerpo.
- [ ] Al seleccionar una variable y presionar insertar, se agrega el placeholder en la posicion del cursor.
- [ ] Si no se puede insertar en el cursor, se agrega al final del cuerpo sin perder texto.
- [ ] Las variables mostradas coinciden con las validadas por el motor.
- [ ] No se permiten variables desconocidas al guardar.
- [ ] La UI usa textos en espanol y no obliga a escribir placeholders a mano.

## Dependencias

- Depende de: REQ-0041.
- Requerido por: configuracion amigable de contratos, pagares y documentos.
