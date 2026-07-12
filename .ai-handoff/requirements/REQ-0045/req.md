# REQ-0045 - ABM Persona: dialogo muy largo, botones Guardar/Cancelar fuera de vista

**Numero:** REQ-0045
**Fecha de creacion:** 2026-07-11
**Estado inicial:** NUEVO
**Prioridad:** media

## Texto Original

ABM Persona: el dialogo de alta/edicion es muy largo y los botones Guardar/Cancelar quedan
fuera de vista (hay que hacer scroll de toda la pagina para llegar a ellos). Ajustar el
layout con scroll interno del cuerpo y pie de botones fijo.

## Objetivo Funcional

Que el dialogo de alta/edicion de persona muestre siempre los botones Guardar/Cancelar al
pie, con el cuerpo del formulario desplazandose internamente sin empujar los botones fuera
de la vista.

## Criterios De Aceptacion

- [x] Los botones Guardar/Cancelar quedan siempre visibles al pie del dialogo.
- [x] El cuerpo del dialogo hace scroll interno sin empujar los botones fuera de vista.
- [x] No se rompe el alta/edicion de persona (fisica y juridica, pestanas y roles).

## Reglas De Negocio

- Cambio de presentacion; no altera la logica de guardado ni la validacion de persona.

## Dependencias

- Depende de: REQ-0043, REQ-0044 (ajustes previos de personas).
- Requerido por: usabilidad del ABM de personas.

## Fuentes Y Trazabilidad

- Reporte de usabilidad del usuario: botones fuera de vista en dialogos largos.
