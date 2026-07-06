-- V12: réplica del estándar ABM a los catálogos (desarrollo autónomo autorizado 2026-07-05).
-- moneda: le faltaba estado (regla 13: toda tabla maestra tiene baja lógica).
-- forma_pago: habilitado (regla 1: activo≠disponible para operaciones nuevas).
ALTER TABLE moneda ADD COLUMN IF NOT EXISTS estado varchar(10) NOT NULL DEFAULT 'ACTIVO'
  CHECK (estado IN ('ACTIVO','INACTIVO'));
ALTER TABLE forma_pago ADD COLUMN IF NOT EXISTS habilitado boolean NOT NULL DEFAULT true;

INSERT INTO entidad (entidad, codigo, descripcion, usuario_creacion, fecha_creacion) VALUES
  ('PANTALLAS', 'monedas',     'Monedas',              'sistema', now()),
  ('PANTALLAS', 'impuestos',   'Impuestos',            'sistema', now()),
  ('PANTALLAS', 'formas-pago', 'Formas de pago',       'sistema', now()),
  ('PANTALLAS', 'listas',      'Listas del sistema',   'sistema', now()),
  ('PANTALLAS', 'parametros',  'Parámetros',           'sistema', now()),
  ('PANTALLAS', 'geografia',   'Geografía (Paraguay)', 'sistema', now())
ON CONFLICT (entidad, codigo) DO NOTHING;
