-- ============================================================================
-- V55 - Activos (REQ-0087): campos para el formulario detallado de LOTES y CASAS/DPTOS.
--   superficie          - superficie del lote (m2)
--   dimensiones_linderos- dimensiones y linderos (texto)
--   cochera             - cantidad de cocheras (1..10)
--   m2_construccion     - metros de construccion
--   medida              - medida (texto)
--   ande_medidor / ande_nis          - servicio ANDE
--   essap_medidor / essap_cta_cte    - servicio ESSAP
-- Columnas simples en activo (RLS por tenant ya vigente). Todas opcionales (no rompen datos).
-- (numero_lote, numero_manzana, cuenta_catastral, numero_finca, observacion, precios, direccion,
--  ubicacion, y tipo_operacion/medidas/anio/cantidad_unidades de V54 ya existen.)
-- ============================================================================

SELECT set_config('app.tenant', '-1', true);

ALTER TABLE activo ADD COLUMN IF NOT EXISTS superficie           numeric(15,2);
ALTER TABLE activo ADD COLUMN IF NOT EXISTS dimensiones_linderos text;
ALTER TABLE activo ADD COLUMN IF NOT EXISTS cochera              integer;
ALTER TABLE activo ADD COLUMN IF NOT EXISTS m2_construccion      numeric(15,2);
ALTER TABLE activo ADD COLUMN IF NOT EXISTS medida               varchar(120);
ALTER TABLE activo ADD COLUMN IF NOT EXISTS ande_medidor         varchar(40);
ALTER TABLE activo ADD COLUMN IF NOT EXISTS ande_nis             varchar(40);
ALTER TABLE activo ADD COLUMN IF NOT EXISTS essap_medidor        varchar(40);
ALTER TABLE activo ADD COLUMN IF NOT EXISTS essap_cta_cte        varchar(40);

ALTER TABLE activo DROP CONSTRAINT IF EXISTS ck_activo_cochera;
ALTER TABLE activo ADD CONSTRAINT ck_activo_cochera
  CHECK (cochera IS NULL OR (cochera BETWEEN 0 AND 10));
