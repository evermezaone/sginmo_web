# REQ-0084 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-13
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Validacion

- `ComprobanteOcrService` extrae texto de PDFs de texto con PDFBox, dependencia Java pura declarada en `pom.xml`.
- Para imagenes/PDF escaneado intenta `tesseract` por CLI si existe en `PATH`; si no existe o falla, devuelve resultado sin texto y no interrumpe el flujo.
- `PortalTransferenciaService#informar()` ejecuta OCR dentro de `try/catch`, actualiza `texto_ocr`, campos normalizados, confianza y motor, sin bloquear el alta del comprobante.
- `V57__portal_transferencia_ocr.sql` agrega campos `ocr_importe`, `ocr_fecha`, `ocr_numero`, `ocr_banco`, `ocr_procesado`, `ocr_motor`; `texto_ocr` y `confianza_ocr` ya estaban reservados en V56.
- `transferencias.xhtml` muestra lectura OCR vs datos declarados en la bandeja.
- No hay autoaplicacion de pagos por OCR; la aplicacion sigue dependiendo del flujo manual/conciliacion posterior.

## Hallazgos

### Bloqueantes

- Ninguno.

### No Bloqueantes

- La pagina `transferencias.xhtml` ya contiene codigo de `REQ-0085`; cualquier problema de conciliacion/importacion CSV debe auditarse en ese REQ.

## Riesgos

- OCR imperfecto por naturaleza. Queda mitigado porque es best-effort y no decide pagos.

## Pruebas Revisadas

- [x] Revision estatica de `ComprobanteOcrService`.
- [x] Revision estatica de `PortalTransferenciaService` en la integracion OCR.
- [x] Revision estatica de `V57__portal_transferencia_ocr.sql`.
- [x] Revision estatica de panel OCR en `transferencias.xhtml`.
- [x] `mvn -q -pl sginmo-web -am clean package` ejecutado desde `Desarrollo` con resultado EXIT 0.

## Pruebas Faltantes

- [ ] Prueba manual con PDF bancario real de texto.
- [ ] Prueba manual con imagen sin Tesseract para confirmar degradacion visual.
