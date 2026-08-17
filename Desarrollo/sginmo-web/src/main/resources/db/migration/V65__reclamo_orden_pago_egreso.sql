-- ============================================================================
-- V65 - REQ-0113: la orden de pago del reclamo engancha al modulo Ingresos/Egresos.
-- Se vincula la orden con el egreso generado (por pagar) y se registra cuando se paga.
-- ============================================================================

ALTER TABLE reclamo_orden_pago ADD COLUMN IF NOT EXISTS egreso    bigint REFERENCES ingreso_egreso(ingreso_egreso);
ALTER TABLE reclamo_orden_pago ADD COLUMN IF NOT EXISTS pagado_en timestamptz;
