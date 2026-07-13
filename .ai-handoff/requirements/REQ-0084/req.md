# REQ-0084 - Portal transferencias Fase 2: OCR y extraccion de campos del comprobante

**Numero:** REQ-0084
**Fecha de creacion:** 2026-07-13
**Estado inicial:** NUEVO
**Prioridad:** media

> Fase 2 de la plataforma de auto-pago por transferencia (plan maestro en REQ-0083/analysis.md).
> Depende de REQ-0083 (Fase 1) ya desplegado.

## Objetivo Funcional

Sobre el comprobante adjuntado en el portal (REQ-0083), ejecutar OCR para extraer los datos de la transferencia
y precargarlos/contrastarlos con lo declarado por el cliente, reduciendo el trabajo del operador en la bandeja.
NO aplica pagos por si solo; solo enriquece el tramite con datos extraidos y su nivel de confianza.

## Alcance Funcional

- Ejecutar OCR sobre el archivo (PDF/imagen) al informarse o al abrir en la bandeja; guardar el texto extraido.
- Extraer, cuando sea posible: importe, moneda, fecha/hora, banco emisor, banco receptor, cuenta destino,
  numero de transaccion, titular/remitente, referencia/concepto.
- Guardar nivel de confianza por campo y resultado global.
- Reglas de parsing configurables por banco (no depender de un unico formato de comprobante).
- En la bandeja: mostrar el texto OCR y los campos extraidos vs. los declarados por el cliente; resaltar
  discrepancias y confianza baja.
- Cuando la confianza es alta, precargar los campos declarados (el operador puede corregir).
- El OCR nunca decide la aplicacion; es insumo para la revision (o para la Fase 3).

## Modelo De Datos

- Extender `portal_pago_transferencia`/`portal_pago_transferencia_archivo` con texto OCR y campos extraidos +
  confianza, o crear `portal_pago_transferencia_ocr` (una fila por archivo) sin romper Fase 1.

## Decision Tomada (motor)

- **Tesseract NO se puede instalar en la VPS** (sin `sudo`; el binario nativo del sistema no esta). Por eso:
  - **PDF de texto** (formato mas comun de comprobante bancario): extraccion con **PDFBox** (Java puro, sin binario).
  - **Imagen (JPG/PNG/WEBP) o PDF escaneado**: se intenta la **CLI de `tesseract`** via ProcessBuilder si esta en el
    PATH; si no, degrada limpio (sin texto) y el operador lo lee a mano. Al instalar el binario `tesseract` se
    activa solo, SIN cambios de codigo (decision confirmada por el usuario: "PDFBox ahora + Tesseract listo").

## Criterios De Aceptacion

- [x] Al informar un comprobante se ejecuta la extraccion y se guarda el texto (texto_ocr) y el motor usado.
- [x] Se extraen y guardan importe, fecha, numero y banco con una confianza global (0..100).
- [x] La bandeja muestra los datos leidos (OCR) vs. lo declarado (importe/fecha/numero/banco) y el motor/confianza.
- [x] El parser de banco usa una lista de bancos y regex generales; agregar bancos no requiere tocar el esquema.
- [x] El OCR es best-effort y NO aplica pagos por si solo (solo insumo para la revision).
- [x] Build `mvn -q clean package` EXIT 0; Flyway V57; smoke 37/37.

## Follow-up

- OCR de imagenes: requiere instalar Tesseract en la VPS (`apt install tesseract-ocr tesseract-ocr-spa`, tarea de
  ops). Con eso, el mismo codigo procesa imagenes automaticamente.
- Reglas de parsing por banco mas finas (por ahora regex generales + lista de bancos).

## Dependencias

- Depende de: REQ-0083 (Fase 1).
- Requerido por: REQ-0085 (la conciliacion usa los campos extraidos).
