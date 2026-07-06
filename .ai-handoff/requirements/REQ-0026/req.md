# REQ-0026 - Generacion de PDFs estandar (OpenPDF, sin JasperReports)

**Estado:** implementado (2026-07-06)

## Objetivo Funcional
DECISION DEL USUARIO: NO usar JasperReports (pesado, para reportes complejos con subreportes/graficos). Se generan PDFs ESTANDAR y directos con OpenPDF (ya en el pom por el dataExporter). Servicio generico reutilizable por todos los reportes.

## Criterios De Aceptacion
- [x] PdfService generico: encabezado (empresa+titulo+fecha+usuario), tabla estandar (cabecera azul, filas alternadas), parrafos, pie; devuelve byte[].
- [x] DescargaBean escribe el PDF en la respuesta HTTP (application/pdf, attachment).
- [x] Sin JasperReports ni plantillas .jrxml; una sola dependencia (openpdf) ya presente.
- [x] Verificado: descarga real de PDF (content-type application/pdf, %PDF-, en la VPS).

## Bloqueo Formal Documentado
Validacion visual del usuario PENDIENTE.
