# REQ-0062 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-12
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Hallazgos Bloqueantes

- Sin hallazgos bloqueantes en la re-auditoria.

## Evidencia de Re-auditoria

- `ReportesConsultaService.cobros` mantiene subtotal por moneda cuando `monedaId` es null, evitando sumar monedas distintas.
- `Reporte` ahora incluye `filtros` con periodo, moneda y limite de exportacion.
- `pdf(Reporte)` imprime los filtros aplicados antes de la tabla y conserva total/subtotal.
- `csv(Reporte)` incluye metadata de reporte/filtros/emision y exporta el total/subtotal al final.
- Busqueda de Jasper/JRXML sin dependencia nueva; se mantiene OpenPDF/CSV.

## Pruebas Revisadas

- Revision estatica de `ReportesConsultaService`, `ReportesBean`, `reportes.xhtml` y `V43__pantalla_reportes.sql`.
- Busqueda de Jasper/JRXML sin dependencia nueva.
- Build Maven previo: `mvn -q clean package` EXIT 0.
