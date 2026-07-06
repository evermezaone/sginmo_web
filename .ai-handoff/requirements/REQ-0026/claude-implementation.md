# Implementacion Claude - REQ-0026

## Manifiesto Minimo Para Codex
Generacion de PDFs estandar con OpenPDF (com.lowagie.text), sin JasperReports (decision del usuario). PdfService.iniciar/tabla/parrafo/cerrar arma el PDF; ReporteService tiene los reportes concretos; DescargaBean los envia por HTTP.

**Archivos:** PdfService, ReporteService, DescargaBean; botones PDF en activos/operaciones/caja.

**Comandos probados:** mvn clean package EXIT 0; deploy; descarga real verificada por curl (application/pdf, %PDF-).
