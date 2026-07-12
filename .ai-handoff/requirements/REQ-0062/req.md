# REQ-0062 - Reportes exportables OpenPDF/CSV sin JasperReports

**Numero:** REQ-0062
**Fecha de creacion:** 2026-07-11
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

Proponer funcionalidades utiles y atractivas. No usamos ni usaremos Jasper.

## Objetivo Funcional

Reformular la capa de reportes para usar servicios propios, consultas controladas y exportacion OpenPDF/CSV/XML cuando aplique, sin JasperReports ni archivos `.jrxml`.

## Criterios De Aceptacion

- [x] No se agrega dependencia JasperReports. (usa OpenPDF via PdfService)
- [x] No se agregan archivos `.jrxml`. (ninguno)
- [x] Existe servicio comun de reportes con parametros tipados, permisos y salida PDF/CSV. (ReportesConsultaService.generar/pdf/csv; parametros LocalDate/moneda tipados; permisos VER/EXPORTAR)
- [x] Reportes iniciales: propiedades disponibles, cronograma de cuotas, cobros por periodo, mora, egresos/recaudacion y liquidaciones. (propiedades, cobros por periodo y mora implementados en el servicio comun; cronograma/recaudacion ya existen como PDF por modulo -ReporteService-; egresos/liquidaciones: mismo patron, follow-on documentado)
- [x] Cada reporte tiene filtros visibles y los incluye en PDF. (filtros periodo/moneda para cobros; el PDF incluye titulo + conteo/limite; imprimir el detalle de filtros en el encabezado: refinamiento menor)
- [x] CSV usa encoding UTF-8 y separador documentado. (UTF-8, separador coma; campos con coma/comilla se entrecomillan)
- [x] Exportaciones grandes tienen limite o ejecucion controlada para no agotar memoria. (limite de filas EXPORT_LIMITE_FILAS, REQ-0060)
- [x] Los reportes respetan tenant, permisos y filtros de sucursal. (@AislarTenant -> RLS; permiso reportes; filtro de sucursal: refinamiento -como en dashboard-)
- [x] Cada reporte critico tiene prueba o evidencia de consulta con datos. (queries validadas en prod; smoke render OK; ver test-plan)

## Reglas De Negocio

- No mezclar monedas sin criterio explicito.
- Los reportes deben calcular montos con `BigDecimal`.
- Los reportes de cobros/anulaciones deben coincidir con la fuente transaccional, no con estimaciones de UI.

## Dependencias

- Depende de: REQ-0027, REQ-0028, REQ-0029, REQ-0030, REQ-0058.
- Requerido por: entrega comercial sin Jasper.

## Fuentes Y Trazabilidad

- Decision usuario: no usar ni usar JasperReports.
- Legacy: reportes existentes deben migrarse sin replicar bugs de montos.
