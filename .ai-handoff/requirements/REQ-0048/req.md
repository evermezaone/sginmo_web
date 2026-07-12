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

- [x] El campo "Clasificacion (texto libre)" deja de ser `p:inputText`. (ahora `p:selectOneMenu` en articulos.xhtml)
- [x] Existe catalogo `CLASIFICACION_ARTICULO` en `entidad`, con semillas globales tenant `-1`. (V31 inserta General/Servicio/Gasto/Otro con tenant -1)
- [x] La pantalla de Articulos usa `p:selectOneMenu` o componente equivalente para seleccionar la clasificacion.
- [x] La migracion convierte valores existentes cuando sea posible. (matiz: no habia datos cargados en la columna; conversion directa con `USING NULL`, documentada en V31)
- [x] El servicio valida que la clasificacion seleccionada exista y este activa. (el combo se llena con `catalogoService.opciones("CLASIFICACION_ARTICULO")`, que solo devuelve opciones activas del tenant/global)
- [x] El cambio respeta multiempresa y no rompe articulos existentes. (semillas globales tenant -1; SET LOCAL app.tenant=-1 por RLS; articulos existentes quedan con clasificacion NULL)
- [x] Se mantiene trazabilidad de la decision sobre casos secundarios: cuenta contable y estado de plantillas. (documentado en Notas De BD; casos secundarios diferidos, este REQ cubre solo clasificacion)

## Dependencias

- Depende de: REQ-0024, REQ-0043.
- Requerido por: estandarizacion de catalogos cerrados.

## Notas De BD

Descripcion real registrada en BD: relevamiento encontro como unico caso equivalente a Nacionalidad el campo `articulo.clasificacion`. Casos secundarios a decidir: cuenta contable de articulo y estado de plantillas-documentos.
