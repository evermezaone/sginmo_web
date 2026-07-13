-- ============================================================================
-- V57 - Portal transferencias (REQ-0084 Fase 2, OCR): campos extraidos del comprobante.
-- El texto extraido (texto_ocr) y la confianza (confianza_ocr) ya existen (reservados en V56).
-- Se agregan los campos normalizados que el parser intenta extraer del texto.
-- ============================================================================

SELECT set_config('app.tenant', '-1', true);

ALTER TABLE portal_pago_transferencia ADD COLUMN IF NOT EXISTS ocr_importe   numeric(15,2);
ALTER TABLE portal_pago_transferencia ADD COLUMN IF NOT EXISTS ocr_fecha     date;
ALTER TABLE portal_pago_transferencia ADD COLUMN IF NOT EXISTS ocr_numero    varchar(60);
ALTER TABLE portal_pago_transferencia ADD COLUMN IF NOT EXISTS ocr_banco     varchar(80);
ALTER TABLE portal_pago_transferencia ADD COLUMN IF NOT EXISTS ocr_procesado boolean NOT NULL DEFAULT false;
ALTER TABLE portal_pago_transferencia ADD COLUMN IF NOT EXISTS ocr_motor     varchar(20);   -- PDF | TESSERACT | NINGUNO
