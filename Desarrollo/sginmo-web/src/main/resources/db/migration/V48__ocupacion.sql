-- ============================================================================
-- V48 - Ocupacion/vacancia (REQ-0072): pantalla propia + parametro de objetivo de
-- ocupacion configurable por empresa (global -1 por defecto). La "regla de alquilable"
-- (precio_alquiler>0 y estado<>VENDIDA) queda documentada en el servicio y el objetivo
-- es parametrizable via parametro_sistema (grupo Gerencia).
-- ============================================================================

SELECT set_config('app.tenant', '-1', true);

-- Objetivo de ocupacion (%) configurable; default global 90.
INSERT INTO parametro_sistema (tenant, clave, valor, descripcion, tipo, grupo, valor_defecto, usuario_creacion, fecha_creacion)
  SELECT -1, 'OCUPACION_OBJETIVO_PCT', '90', 'Objetivo de ocupacion de alquileres (%)', 'ENTERO', 'Gerencia', '90', 'sistema', now()
  WHERE NOT EXISTS (SELECT 1 FROM parametro_sistema p WHERE p.clave='OCUPACION_OBJETIVO_PCT' AND p.tenant=-1);

-- Pantalla de ocupacion (permiso propio; se apoya en el perfil gerencial).
INSERT INTO entidad (lista, codigo, tenant, descripcion, usuario_creacion, fecha_creacion)
VALUES ('PANTALLAS', 'ocupacion', -1, 'Ocupacion y vacancia', 'sistema', now())
ON CONFLICT (lista, codigo, tenant) DO NOTHING;
