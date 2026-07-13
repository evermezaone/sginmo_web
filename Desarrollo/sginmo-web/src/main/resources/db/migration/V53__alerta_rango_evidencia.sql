-- ============================================================================
-- V53 - Alertas gerenciales (REQ-0075 ronda 2, obs 297): la evidencia de una alerta derivada de
-- un objetivo debe abrir con el MISMO rango que disparo la alerta (mensual/trimestral/anual/
-- personalizado), no con "mes actual" fijo. Se persiste el rango real por alerta.
-- ============================================================================

SELECT set_config('app.tenant', '-1', true);

ALTER TABLE alerta_gerencial ADD COLUMN IF NOT EXISTS drill_desde date;
ALTER TABLE alerta_gerencial ADD COLUMN IF NOT EXISTS drill_hasta date;
