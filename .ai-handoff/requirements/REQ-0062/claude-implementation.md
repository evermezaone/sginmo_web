# REQ-0062 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0062
- Tipo de cambio: backend + UI + BD (solo pantalla)
- Riesgo: bajo-medio (solo lectura; export controlado)
- Archivos clave:
  - `V43__pantalla_reportes.sql`: registra pantalla `reportes`.
  - `servicio/ReportesConsultaService.java`: @AislarTenant; servicio comun (generar/pdf/csv) con parametros tipados (LocalDate/moneda), permisos (VER/EXPORTAR), limite de filas (EXPORT_LIMITE_FILAS), sin Jasper. Reportes: propiedades, cobros por periodo, mora.
  - `web/ReportesBean.java` + `webapp/reportes.xhtml`: selector de reporte + filtros + PDF/CSV.
  - `WEB-INF/plantilla.xhtml`, `tools/smoke-test-vps.py`: menu + cobertura.
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - V43 en `BEGIN...ROLLBACK`: pantalla registrada.
  - Deploy + Flyway V43 `success=t`; `python tools/smoke-test-vps.py`: 28/28 RENDER OK incl. `reportes`.
- Cambios de datos: si, V43 (solo pantalla).
- Cambios de entorno: no.
- Impacto LLM/tokens: no.
- Decision esperada: aprobar (riesgo bajo-medio) + revisar diferidos.
- Notas para auditor:
  - Sin Jasper: reutiliza PdfService (OpenPDF). `grep -ri jasper */pom.xml` -> sin resultados; sin .jrxml.
  - "No mezclar monedas": cobros filtra por moneda; montos BigDecimal.
  - "Coincide con la fuente transaccional": cobros salen de la tabla cobro (estado ACTIVO), no de UI.
  - Limite: EXPORT_LIMITE_FILAS (REQ-0060) via setMaxResults.

## Resumen Funcional

Nueva pantalla "Reportes": elegir reporte (propiedades disponibles / cobros por periodo / mora), aplicar
filtros (periodo/moneda) y exportar en PDF o CSV. Sin JasperReports.

## Resumen Tecnico

ReportesConsultaService @AislarTenant arma un Reporte tabular (titulo/columnas/filas) con consultas
controladas y lo exporta a PDF (PdfService) o CSV (UTF-8). Respeta RLS, permisos y limite de filas.

## Limitaciones Conocidas (transparencia)

- Reportes cronograma/recaudacion: ya existen como PDF por modulo (ReporteService, REQ-0027-0029);
  egresos/liquidaciones en el servicio comun: follow-on con el mismo patron (Reporte -> pdf/csv).
- Filtro de sucursal: refinamiento (como en el dashboard).
- Detalle de filtros impreso en el encabezado del PDF: refinamiento (hoy: titulo + conteo/limite).

## Archivos Modificados

Ver Manifiesto. V43 nueva.

## Cambios De Datos

V43: registra pantalla `reportes`. Sin cambios de datos de negocio.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; V43 rollback OK; deploy + Flyway success; smoke 28/28. Ver test-plan.

## Pruebas Manuales Sugeridas

1. Reportes -> Cobros por periodo -> elegir mes + moneda -> PDF y CSV. Propiedades -> PDF/CSV. Mora -> PDF/CSV.

## Riesgos Conocidos

- Solo lectura; export limitado. Ver "Limitaciones".
