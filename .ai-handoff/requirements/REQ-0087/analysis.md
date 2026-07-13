# REQ-0087 - Analisis y plan (reestructura del formulario de Activos con combo GENERAR)

## Alcance (del pedido)

Combo "GENERAR:" (LOTES / CASAS-DPTOS) que cambia el formulario, con reglas distintas segun sea GENERACION
(alta) o EDICION:
- GENERAR=LOTES (alta): mantener la pantalla actual de generacion masiva de lotes (carga rapida).
- GENERAR=CASAS/DPTOS (alta): mostrar el formulario detallado de casas/dptos.
- EDITAR un lote existente: mostrar el formulario detallado de LOTES.
- EDITAR una casa/dpto: formulario detallado de casas/dptos.

Formulario LOTES (edicion): nro_lote, manzana, tipo (default "Lote"), loteamiento (combo), cuenta catastral,
nro finca/matricula, superficie, dimensiones y linderos, ubicacion en cascada, direccion, precio, comision,
observacion amplia, adjuntos (documentos + imagenes).

Formulario CASAS/DPTOS: nombre, tipo (todas menos Lote/Loteamiento), complejo/edificio/salones (combo de
activos que no sean Loteamiento), cuenta catastral, nro finca, cochera (1..10), m2 construccion, medida,
servicios ANDE (medidor, NIS) y ESSAP (nro medidor, cta cte), ubicacion en cascada, direccion, precio,
comision, caracteristicas/observacion.

## Decisiones de modelo (propuestas)

1. **Loteamiento / Complejo = el `padre` existente.** "Loteamientos existentes" = activos con tipo
   Loteamiento; "Complejo/Edificio/Salones" = activos que NO son Loteamiento. Se reutiliza `activo.padre`
   (ya existe) + filtros por tipo en los combos. No se crea una entidad nueva.
2. **Tipo por defecto "Lote":** al generar LOTES el tipo se fija a la opcion Lote del catalogo TIPOS_ACTIVO.
3. **Adjuntos:** reutilizar `documento_adjunto` con `entidad_tipo='ACTIVO'` (REQ-0053), que admite documentos e
   imagenes; el componente de carga ya existe en el modulo de documentos.
4. **Columnas nuevas en `activo` (V55):** `superficie` numeric, `dimensiones_linderos` text, `cochera` int
   (1..10), `m2_construccion` numeric, `medida` varchar, `ande_medidor` varchar, `ande_nis` varchar,
   `essap_medidor` varchar, `essap_cta_cte` varchar. (numero_lote, numero_manzana, cuenta_catastral,
   numero_finca, observacion, precio/comision, direccion, ubicacion ya existen; tipo_operacion/medidas/anio/
   cantidad_unidades llegan en REQ-0088.)
5. **UI:** el combo GENERAR vive arriba del dialogo; con `p:ajax` alterna entre fragmentos (generar-lotes,
   form-casas, form-lote-edicion) segun modo (alta/edicion) y seleccion. Reutiliza la cascada de ubicacion y
   los precios ya presentes.

## Riesgos / notas

- Es el REQ mas grande de esta tanda (migracion + reestructura de activos.xhtml + logica de modo en ActivoBean).
- Interactua con REQ-0088 (campos ya agregados) y con la generacion masiva de lotes existente (no romperla).
- Sugerencia: construir en un paso, con smoke de render de activos y prueba manual de alta/edicion por tipo.

## Plan de implementacion

1. V55: columnas nuevas en activo.
2. Activo entity: campos + getters/setters.
3. ActivoBean: `generarTipo` (LOTES/CASAS_DPTOS), logica de modo (alta vs edicion), combos de loteamiento y de
   complejo (filtrados por tipo), cochera 1..10, carga de adjuntos.
4. activos.xhtml: combo GENERAR + fragmentos condicionales (generacion masiva de lotes / form casas-dptos /
   form lote-edicion) con secciones DATOS, UBICACION, SERVICIOS (ANDE/ESSAP), COMERCIALES, ADJUNTOS/OBS.
5. Build + deploy + smoke + prueba manual + derivar.
