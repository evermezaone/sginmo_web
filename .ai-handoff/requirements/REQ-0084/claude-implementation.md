# REQ-0084 (Fase 2) - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-13
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0084 (Fase 2 OCR de la plataforma de transferencia; depende de REQ-0083 Fase 1).
- Tipo de cambio: dependencia (PDFBox) + BD (V57) + servicio de OCR + integracion en informar + UI bandeja.
- Riesgo: bajo-medio (extraccion best-effort; no aplica pagos).
- Decision de motor: Tesseract no instalable en la VPS (sin sudo). PDF de texto -> PDFBox (Java puro);
  imagenes/PDF escaneado -> CLI `tesseract` si esta en el PATH (ProcessBuilder), si no degrada. Confirmado por el usuario.
- Archivos clave:
  - `sginmo-web/pom.xml`: dependencia `org.apache.pdfbox:pdfbox:3.0.3` (sin binario nativo).
  - `V57__portal_transferencia_ocr.sql`: columnas ocr_importe, ocr_fecha, ocr_numero, ocr_banco, ocr_procesado,
    ocr_motor (texto_ocr y confianza_ocr ya existian de V56).
  - `servicio/ComprobanteOcrService.java`: extraer(bytes, mime) -> PDF via PDFBox (PDFTextStripper); imagen o PDF
    escaneado via CLI tesseract (guardada: IOException si no esta instalada -> degrada). parsear(): regex de
    importe/fecha/numero + lista de bancos; confianza = campos_encontrados*25.
  - `servicio/PortalTransferenciaService.java`: en informar(), tras guardar, corre el OCR best-effort y actualiza
    la fila (texto/campos/confianza/motor); la bandeja expone los campos OCR en la Fila.
  - `webapp/transferencias.xhtml`: seccion "Lectura del comprobante (OCR)" en el dialogo de revision (leido vs
    declarado + motor/confianza).
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - Deploy VPS + Flyway V57 (schema v57); `python tools/smoke-test-vps.py`: 37/37 (transferencias 200).
- Cambios de datos: si, V57 (columnas OCR). Cambios de entorno: opcional (instalar tesseract habilita imagenes).
- Decision esperada: aprobar Fase 2; conciliacion bancaria en REQ-0085.
- Notas para auditor:
  - OCR best-effort dentro de informar(): un fallo del OCR no interrumpe el alta (try/catch).
  - No hay dependencia dura de Tesseract: el WAR no lo requiere; la CLI se usa solo si existe.
  - No aplica pagos: el OCR solo llena campos-insumo para la revision (Fase 1 sigue decidiendo la aplicacion).

## Resumen Funcional

Al informar una transferencia, el sistema lee el comprobante (texto de PDF con PDFBox; imagenes via Tesseract si
esta instalado) y extrae importe/fecha/numero/banco con una confianza. La bandeja muestra lo leido vs lo declarado.

## Resumen Tecnico

ComprobanteOcrService (PDFBox + CLI tesseract opcional) + columnas OCR en portal_pago_transferencia + panel en la bandeja.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| sginmo-web/pom.xml | dependencia pdfbox 3.0.3 |
| V57__portal_transferencia_ocr.sql | columnas ocr_* |
| servicio/ComprobanteOcrService.java | NUEVO (PDFBox + tesseract CLI + parser) |
| servicio/PortalTransferenciaService.java | OCR en informar + campos OCR en la bandeja |
| webapp/transferencias.xhtml | panel OCR (leido vs declarado) |

## Cambios De Datos

V57: ocr_importe/ocr_fecha/ocr_numero/ocr_banco/ocr_procesado/ocr_motor en portal_pago_transferencia.

## Variables De Entorno

Opcional: instalar `tesseract` (+ `tesseract-ocr-spa`) en la VPS habilita el OCR de imagenes. TESSERACT_LANG (default spa+eng).

## Pruebas Ejecutadas

Build OK; deploy VPS; Flyway V57; smoke 37/37.

## Pruebas Manuales Sugeridas

1. Informar una transferencia con comprobante PDF de texto -> en la bandeja, la seccion OCR muestra importe/fecha/nro/banco leidos.
2. Con comprobante imagen y sin tesseract instalado -> OCR "sin lectura automatica" (degrada); el operador lee el adjunto.

## Limitaciones Conocidas

- OCR de imagenes requiere instalar Tesseract en la VPS (ops). Parsing por regex generales + lista de bancos.

## Riesgos Conocidos

- Extraccion imperfecta: es insumo, no decide; la aplicacion sigue siendo manual (Fase 1) hasta la conciliacion (0085).
