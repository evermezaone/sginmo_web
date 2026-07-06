-- V18 (REQ-0016..0023): pantallas de operaciones y caja.
INSERT INTO entidad (entidad, codigo, descripcion, usuario_creacion, fecha_creacion) VALUES
  ('PANTALLAS', 'operaciones', 'Operaciones (alquiler/venta)', 'sistema', now()),
  ('PANTALLAS', 'caja',        'Caja y cobros',                'sistema', now());
