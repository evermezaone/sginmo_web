-- ============================================================================
-- SGInmo Web — V22: baja logica de propietarios de activo (REQ-0013 obs 211/212)
-- activo_propietario pasa a tener estado ACTIVO/INACTIVO para preservar la
-- trazabilidad historica (el ABM solo lista/valida los ACTIVOS).
-- ============================================================================

ALTER TABLE activo_propietario
  ADD COLUMN IF NOT EXISTS estado varchar(10) NOT NULL DEFAULT 'ACTIVO';

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'activo_propietario_estado_check'
          AND conrelid = 'activo_propietario'::regclass
    ) THEN
        ALTER TABLE activo_propietario
          ADD CONSTRAINT activo_propietario_estado_check
          CHECK (estado IN ('ACTIVO', 'INACTIVO'));
    END IF;
END $$;
