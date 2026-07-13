# REQ-0088 - Activos: campos nuevos (Operacion, Medidas, Anio, Cantidad de unidades) + renombrar contenedor a Caracteristicas

**Numero:** REQ-0088
**Fecha de creacion:** 2026-07-13
**Estado inicial:** NUEVO
**Prioridad:** media

## Texto Original

Agregar en el formulario de Activos Inmobiliarios (NUEVO y EDITAR):
- Tipo de Operacion (combo Alquiler/Venta) con etiqueta "Operacion o Tipo de Contrato".
- Medidas (texto/num).
- Anio (numerico).
- Cantidad de Unidades (numerico entero).
Y renombrar la seccion/campo "Contenedor (edificio/loteamiento - opcional)" a "Caracteristicas".

## Objetivo Funcional

Que la carga y edicion de activos permita registrar la operacion (Alquiler/Venta), las medidas, el anio y la cantidad de unidades; y que la etiqueta del contenedor pase a llamarse "Caracteristicas".

## Alcance

- V54: columnas `tipo_operacion` (CHECK ALQUILER/VENTA), `medidas`, `anio`, `cantidad_unidades` en `activo`.
- Entidad `Activo`: campos + getters/setters (JPA persiste solo via merge/persist existente).
- `activos.xhtml` (tab Datos): combo Operacion (Alquiler/Venta), Medidas, Anio, Cantidad de unidades; y label del autoComplete de contenedor renombrado a "Caracteristicas".

## Criterios De Aceptacion

- [x] El formulario (NUEVO y EDITAR) muestra Operacion (Alquiler/Venta), Medidas, Anio y Cantidad de unidades.
- [x] Los valores se guardan y se recuperan al reeditar.
- [x] La etiqueta del contenedor dice "Caracteristicas".
- [x] `tipo_operacion` valida ALQUILER/VENTA (CHECK) y admite nulo.
- [x] Build `mvn -q clean package` EXIT 0; Flyway V54; smoke 36/36 (activos 200).

## Dependencias

- Depende de: REQ-0014 (activos/propiedades).
- Relacionado: REQ-0087 (reestructura del formulario de activos).
