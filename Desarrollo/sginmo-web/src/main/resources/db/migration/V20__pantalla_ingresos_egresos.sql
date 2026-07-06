-- V20 (REQ-0024): pantalla de ingresos/egresos + seed de tipos de imputacion si falta.
INSERT INTO entidad (entidad, codigo, descripcion, usuario_creacion, fecha_creacion) VALUES
  ('PANTALLAS', 'ingresos-egresos', 'Ingresos y egresos', 'sistema', now());

INSERT INTO entidad (entidad, codigo, descripcion, usuario_creacion, fecha_creacion)
SELECT 'TIPOS_IMPUTACION', v.codigo, v.descripcion, 'sistema', now()
FROM (VALUES ('GASTO','Gasto operativo'),('OTRO_INGRESO','Otro ingreso'),
             ('SERVICIO','Servicio'),('IMPUESTO','Impuesto/tasa')) AS v(codigo,descripcion)
WHERE NOT EXISTS (SELECT 1 FROM entidad e WHERE e.entidad='TIPOS_IMPUTACION' AND e.codigo=v.codigo);
