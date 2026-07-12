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

- [x] El dialogo de `operaciones.xhtml` usa cuerpo con scroll interno. (div wrapper `max-height:60vh; overflow-y:auto`)
- [x] Los botones Guardar/Cancelar quedan fijos al pie del dialogo. (`div.pie-dialogo` fuera del cuerpo desplazable)
- [x] El ajuste funciona en escritorio y celular. (dialog `responsive="true"`; alto relativo en vh)
- [x] No se rompe la validacion ni el submit de operacion. (no se toco el `commandButton` de "Registrar operacion" ni el `actionListener`; smoke render OK)
- [x] La solucion reutiliza el patron aplicado en REQ-0045 para Persona. (mismo wrapper de scroll + clase `pie-dialogo`)
- [x] No hay superposicion de botones con campos del formulario. (pie separado del cuerpo por marcado, no superpuesto/absoluto)

## Dependencias

- Depende de: REQ-0016, REQ-0045.
- Requerido por: usabilidad de alta de operaciones.
