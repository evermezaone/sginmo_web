# REQ-0048 - Articulo: campo Clasificacion debe ser lista de catalogo

**Numero:** REQ-0048
**Fecha de creacion:** 2026-07-12
**Estado inicial:** ESPERA_USUARIO
**Prioridad:** media

## Texto Original

Requerimiento creado por Claude en BD luego del relevamiento asociado a Nacionalidad/Persona.

## Objetivo Funcional

Convertir `articulo.clasificacion`, hoy texto libre en `articulos.xhtml`, en una lista seleccionable desde catalogo `entidad`, igual al patron definido para Nacionalidad.

## Criterios De Aceptacion

- [ ] El campo "Clasificacion (texto libre)" deja de ser `p:inputText`.
- [ ] Existe catalogo `CLASIFICACION_ARTICULO` en `entidad`, con semillas globales tenant `-1`.
- [ ] La pantalla de Articulos usa `p:selectOneMenu` o componente equivalente para seleccionar la clasificacion.
- [ ] La migracion convierte valores existentes cuando sea posible.
- [ ] El servicio valida que la clasificacion seleccionada exista y este activa.
- [ ] El cambio respeta multiempresa y no rompe articulos existentes.
- [ ] Se mantiene trazabilidad de la decision sobre casos secundarios: cuenta contable y estado de plantillas.

## Dependencias

- Depende de: REQ-0024, REQ-0043.
- Requerido por: estandarizacion de catalogos cerrados.

## Notas De BD

Descripcion real registrada en BD: relevamiento encontro como unico caso equivalente a Nacionalidad el campo `articulo.clasificacion`. Casos secundarios a decidir: cuenta contable de articulo y estado de plantillas-documentos.
