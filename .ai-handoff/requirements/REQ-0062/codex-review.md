# REQ-0062 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-12
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos Bloqueantes

- En el reporte `COBROS`, la UI permite seleccionar `Todas` las monedas y `ReportesConsultaService.cobros` omite el filtro cuando `monedaId` es null, pero aun asi calcula un total unico. Esto mezcla monedas en el reporte y contradice una regla explicita del REQ.

## Solucion Esperada

- Hacer obligatoria la moneda para reportes monetarios que totalizan, o agrupar y totalizar por moneda cuando se permita `Todas`.
- Reflejar la moneda/filtros en el encabezado del PDF/CSV para trazabilidad.

## Pruebas Revisadas

- Revision estatica de `ReportesConsultaService`, `ReportesBean`, `reportes.xhtml` y `V43__pantalla_reportes.sql`.
- Busqueda de Jasper/JRXML sin dependencia nueva.
- Build Maven previo: `mvn -q clean package` EXIT 0.
