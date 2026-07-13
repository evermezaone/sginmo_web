# REQ-0087 - Activos: combo GENERAR (LOTES / CASAS-DPTOS) con formulario dinamico y campos detallados

**Numero:** REQ-0087
**Fecha de creacion:** 2026-07-13
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

Reestructurar el flujo de Activos: un combo "GENERAR:" (LOTES / CASAS-DPTOS) que cambia el formulario, con reglas
distintas en alta vs edicion. Formularios detallados de LOTES (al editar un lote) y de CASAS/DPTOS (alta y edicion),
con datos de servicios (ANDE/ESSAP), cochera, m2, medida, superficie, dimensiones, ubicacion en cascada, comerciales,
observacion/caracteristicas y adjuntos. Ver el texto completo del pedido en el historico del REQ.

## Objetivo Funcional

En la pantalla de activos, un combo "GENERAR:" (solo en alta) alterna entre generacion masiva de LOTES y el
formulario detallado de CASAS/DPTOS. Al editar un lote se muestra el formulario detallado de LOTE; al editar
una casa/dpto, su formulario detallado. Ver plan y decisiones de modelo en `analysis.md`.

## Alcance Implementado

- **V55**: columnas en `activo`: superficie, dimensiones_linderos, cochera (CHECK 0..10), m2_construccion, medida,
  ande_medidor, ande_nis, essap_medidor, essap_cta_cte.
- **Entidad Activo**: campos + getters/setters (persisten via el merge/persist existente).
- **ActivoBean**: combo `generarTipo` (LOTES/CASAS_DPTOS); helpers de modo (esNuevo, esLote, modoGenerarLotes,
  modoLoteDetalle, modoCasaDpto); `tiposFormulario` (excluye Lote/Loteamiento en casa/dpto); cocheras 1..10;
  `guardarDialogo()` (en alta LOTES genera masivamente, si no guarda el activo).
- **activos.xhtml**: combo GENERAR arriba del dialogo (solo alta); fragmento de generacion masiva embebido cuando
  GENERAR=LOTES; formulario detallado con secciones condicionales — Datos del lote (superficie, dimensiones) al
  editar lote; Datos inmobiliaria (cochera, m2, medida) + Datos servicios (ANDE, ESSAP) en casa/dpto. Reutiliza la
  cascada de ubicacion, precios/comisiones, catastro (nro lote/manzana/cuenta catastral/nro finca), observacion y
  el contenedor "Caracteristicas" (padre) como loteamiento/complejo.

## Criterios De Aceptacion

- [x] En alta, el combo GENERAR ofrece LOTES y CASAS/DPTOS y cambia el formulario.
- [x] GENERAR=LOTES (alta) muestra la generacion masiva de lotes (misma logica existente) y "Generar" la ejecuta.
- [x] GENERAR=CASAS/DPTOS (alta) muestra el formulario detallado con Tipo (sin Lote/Loteamiento), cochera (1..10),
      m2, medida, y servicios ANDE (medidor/NIS) y ESSAP (nro medidor/cta cte).
- [x] Al EDITAR un lote se muestra el formulario detallado de LOTE (superficie, dimensiones/linderos; el tipo queda
      fijo en Lote) ademas de catastro/ubicacion/comerciales/observacion.
- [x] Los campos nuevos se guardan y se recuperan al reeditar.
- [x] Build `mvn -q clean package` EXIT 0; Flyway V55; smoke 36/36 (activos 200).

## Follow-up (no incluido en esta entrega)

- Componente de **adjuntos** inline en el dialogo (documentos + imagenes): el modulo de Documentos (REQ-0053) ya
  gestiona adjuntos por entidad ACTIVO; integrarlo dentro del dialogo de activo se propone como REQ aparte.
- Combos dedicados y etiquetados de "Loteamiento" (lote) y "Complejo/Edificio/Salones" (casa) filtrados por tipo:
  hoy se usa el autoComplete de contenedor "Caracteristicas" (padre). Se puede especializar en un REQ menor.

## Dependencias

- Depende de: REQ-0014/0015 (activos y generacion de lotes), REQ-0088 (campos operacion/medidas/anio/cantidad).
