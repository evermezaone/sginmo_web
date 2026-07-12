# REQ-0062 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-12
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos Bloqueantes

- La correccion evita mezclar monedas: cuando `monedaId` es null, `ReportesConsultaService.cobros` calcula `Totales por moneda`. Esa parte queda validada.
- Sigue incumplido el criterio de aceptacion "Cada reporte tiene filtros visibles y los incluye en PDF": `ReportesConsultaService.pdf` solo incluye empresa, titulo y cantidad de filas/limite, pero no periodo, moneda seleccionada ni filtros aplicados. `ReportesConsultaService.csv` tampoco incluye metadata ni subtotal por moneda, por lo que una exportacion descargada queda sin trazabilidad suficiente.

## Solucion Esperada

- Mantener la logica actual de subtotales por moneda cuando se seleccione `Todas`.
- Agregar al modelo `Reporte` una descripcion de filtros aplicados, por ejemplo periodo y moneda (`Todas` o descripcion), y usarla en el encabezado del PDF.
- Incluir en CSV una seccion inicial comentada/metadata o filas de encabezado documentadas con fecha de generacion, usuario, periodo, moneda y limite aplicado; incluir tambien el total/subtotal exportado.

## Pruebas Revisadas

- Revision estatica de `ReportesConsultaService`, `ReportesBean`, `reportes.xhtml` y `V43__pantalla_reportes.sql`.
- Busqueda de Jasper/JRXML sin dependencia nueva.
- Build Maven previo: `mvn -q clean package` EXIT 0.
