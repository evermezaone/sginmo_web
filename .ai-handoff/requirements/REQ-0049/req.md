# REQ-0049 - Alta de Operacion: botones siempre visibles

**Numero:** REQ-0049
**Fecha de creacion:** 2026-07-12
**Estado inicial:** ESPERA_USUARIO
**Prioridad:** media

## Texto Original

En el dialogo de alta de Operacion, los botones de accion quedan fuera de vista. Aplicar el mismo ajuste que en Persona.

## Objetivo Funcional

Ajustar el dialogo de alta/edicion de Operacion para que Guardar/Cancelar queden siempre visibles, con cuerpo desplazable y pie fijo.

## Criterios De Aceptacion

- [ ] El dialogo de `operaciones.xhtml` usa cuerpo con scroll interno.
- [ ] Los botones Guardar/Cancelar quedan fijos al pie del dialogo.
- [ ] El ajuste funciona en escritorio y celular.
- [ ] No se rompe la validacion ni el submit de operacion.
- [ ] La solucion reutiliza el patron aplicado en REQ-0045 para Persona.
- [ ] No hay superposicion de botones con campos del formulario.

## Dependencias

- Depende de: REQ-0016, REQ-0045.
- Requerido por: usabilidad de alta de operaciones.
