-- ============================================================================
-- V40 - Arqueo y cierre controlado de caja (REQ-0059)
-- Extiende la planilla existente (no la reemplaza) con datos de arqueo:
-- efectivo esperado/contado, diferencia, observacion y reapertura trazable.
-- ============================================================================

SELECT set_config('app.tenant', '-1', true);

ALTER TABLE planilla
  ADD COLUMN IF NOT EXISTS efectivo_esperado  numeric(15,2),
  ADD COLUMN IF NOT EXISTS efectivo_contado   numeric(15,2),
  ADD COLUMN IF NOT EXISTS diferencia         numeric(15,2),
  ADD COLUMN IF NOT EXISTS observacion_cierre varchar(300),
  ADD COLUMN IF NOT EXISTS reabierta          boolean DEFAULT false,
  ADD COLUMN IF NOT EXISTS usuario_reapertura varchar(20),
  ADD COLUMN IF NOT EXISTS fecha_reapertura   timestamptz,
  ADD COLUMN IF NOT EXISTS motivo_reapertura  varchar(250);

INSERT INTO entidad (lista, codigo, tenant, descripcion, usuario_creacion, fecha_creacion)
VALUES ('PANTALLAS', 'arqueo', -1, 'Arqueo de caja', 'sistema', now())
ON CONFLICT (lista, codigo, tenant) DO NOTHING;
