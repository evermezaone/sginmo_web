-- V21 (REQ-0025): pantalla de liquidaciones + seed de motivos si falta.
INSERT INTO entidad (entidad, codigo, descripcion, usuario_creacion, fecha_creacion) VALUES
  ('PANTALLAS', 'liquidaciones', 'Liquidaciones', 'sistema', now())
ON CONFLICT (entidad, codigo) DO NOTHING;

INSERT INTO entidad (entidad, codigo, descripcion, usuario_creacion, fecha_creacion)
SELECT 'MOTIVOS_LIQUIDACION', v.codigo, v.descripcion, 'sistema', now()
FROM (VALUES ('FIN_CONTRATO','Fin de contrato'),('RESCISION','Rescisión anticipada'),
             ('MUTUO','Acuerdo mutuo')) AS v(codigo,descripcion)
WHERE NOT EXISTS (SELECT 1 FROM entidad e WHERE e.entidad='MOTIVOS_LIQUIDACION' AND e.codigo=v.codigo);
