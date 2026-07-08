# REQ-0026 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-08
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

- Obs 234: `PdfService` no implementa pie/paginado. El criterio de aceptacion de REQ-0026 pide servicio generico con encabezado, tabla, parrafos y pie; el propio comentario de `PdfService` dice “pie con paginado”, pero el codigo solo crea encabezado/contenido y cierra el documento. No hay `PdfPageEvent`, `HeaderFooter` ni texto de pie. Impacto: todos los PDFs estandar salen sin pie uniforme ni numeracion, y la infraestructura reusable queda incompleta.

### No Bloqueantes

- Usa OpenPDF (`com.lowagie`) y no se encontraron dependencias Jasper ni plantillas `.jrxml`.
- `DescargaBean` escribe `application/pdf`, `Content-Disposition: attachment`, longitud y completa la respuesta.
- Los botones consumidores usan `ajax=false` en activos, operaciones y caja.

## Riesgos

- Reportes sin identificador/paginacion uniforme al imprimirse o archivarse.

## Pruebas Revisadas

- [x] Revision estatica de `PdfService`.
- [x] Revision estatica de `ReporteService`.
- [x] Revision estatica de `DescargaBean`.
- [x] Barrido de Jasper/jrxml: no hay dependencias ni plantillas.
- [x] Revision de botones PDF en `activos.xhtml`, `operaciones.xhtml` y `caja.xhtml`.
- [x] `mvn -q clean package` en `Desarrollo` con JDK 23: EXIT 0.

## Pruebas Faltantes

- [ ] Reejecutar `mvn -q clean package` luego de corregir.
- [ ] Prueba funcional: generar PDF de mas de una pagina y verificar pie/numeracion en cada pagina.
