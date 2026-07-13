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

## Decision Abierta (confirmar al iniciar)

- **Motor OCR:** recomendacion Tesseract via `tess4j` (local, sin costo, offline; requiere instalar Tesseract en
  la VPS) frente a un servicio cloud (mejor precision pero costo + datos afuera). Impacta build/deploy y entorno.

## Criterios De Aceptacion

- [ ] Al informar/abrir un comprobante, se ejecuta OCR y se guarda el texto extraido.
- [ ] Se extraen y guardan los campos disponibles con confianza por campo.
- [ ] La bandeja muestra OCR vs. declarado y resalta discrepancias/confianza baja.
- [ ] Reglas de parsing configurables por banco; agregar un banco no requiere recompilar reglas hardcodeadas.
- [ ] El OCR no aplica pagos por si solo.
- [ ] Build `mvn -q clean package` EXIT 0.

## Dependencias

- Depende de: REQ-0083 (Fase 1).
- Requerido por: REQ-0085 (la conciliacion usa los campos extraidos).
