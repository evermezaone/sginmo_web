-- ============================================================================
-- V54 - Activos inmobiliarios (REQ-0088): campos nuevos en la carga/edicion de activos.
--   tipo_operacion  (ALQUILER | VENTA)  - operacion / tipo de contrato del activo
--   medidas         - dimensiones del activo (texto libre)
--   anio            - anio de construccion / fabricacion
--   cantidad_unidades - cantidad de unidades (bloques/complejos)
-- Columnas simples en la tabla activo (RLS ya vigente por tenant). Sin defaults que rompan datos.
-- ============================================================================

SELECT set_config('app.tenant', '-1', true);

ALTER TABLE activo ADD COLUMN IF NOT EXISTS tipo_operacion    varchar(10);
ALTER TABLE activo ADD COLUMN IF NOT EXISTS medidas           varchar(120);
ALTER TABLE activo ADD COLUMN IF NOT EXISTS anio              integer;
ALTER TABLE activo ADD COLUMN IF NOT EXISTS cantidad_unidades integer;

ALTER TABLE activo DROP CONSTRAINT IF EXISTS ck_activo_tipo_operacion;
ALTER TABLE activo ADD CONSTRAINT ck_activo_tipo_operacion
  CHECK (tipo_operacion IS NULL OR tipo_operacion IN ('ALQUILER','VENTA'));
