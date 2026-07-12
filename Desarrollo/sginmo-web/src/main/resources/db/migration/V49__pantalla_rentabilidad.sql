-- ============================================================================
-- V49 - Rentabilidad gerencial (REQ-0071): pantalla propia. La rentabilidad base-caja
-- se calcula desde ingreso_egreso (ledger de ingresos/egresos, moneda base de la empresa;
-- REQ-0024), clasificado por articulo.aplicacion (no por textos hardcodeados). El deposito
-- de garantia se clasifica como pasivo/terceros y NO cuenta como rentabilidad.
-- ============================================================================

SELECT set_config('app.tenant', '-1', true);

INSERT INTO entidad (lista, codigo, tenant, descripcion, usuario_creacion, fecha_creacion)
VALUES ('PANTALLAS', 'rentabilidad', -1, 'Rentabilidad gerencial', 'sistema', now())
ON CONFLICT (lista, codigo, tenant) DO NOTHING;
