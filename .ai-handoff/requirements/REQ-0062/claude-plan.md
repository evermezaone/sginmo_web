# REQ-0062 - Plan De Implementacion

**Estado:** APROBADO_PARA_DESARROLLO
**Fecha:** 2026-07-12

## Estrategia

Servicio comun ReportesConsultaService (generar/pdf/csv) con parametros tipados, permisos y limite de
filas (parametro EXPORT_LIMITE_FILAS). Reutiliza PdfService (OpenPDF, sin Jasper). Reportes: propiedades,
cobros por periodo, mora.

## Archivos A Modificar

| Archivo | Cambio |
|---|---|
| V43__pantalla_reportes.sql | registra pantalla |
| servicio/ReportesConsultaService.java | NUEVO — servicio comun PDF/CSV |
| web/ReportesBean.java + webapp/reportes.xhtml | NUEVOS |
| WEB-INF/plantilla.xhtml + smoke | menu + cobertura |

## Pruebas Previstas

- [ ] Build OK
- [ ] V43 rollback + deploy + smoke
- [ ] Sin Jasper; CSV UTF-8; limite de filas

## Riesgos

- Bajo-medio: solo lectura. No mezclar monedas (filtro moneda); BigDecimal.

## Cambios De Datos

V43 registra pantalla `reportes`.
