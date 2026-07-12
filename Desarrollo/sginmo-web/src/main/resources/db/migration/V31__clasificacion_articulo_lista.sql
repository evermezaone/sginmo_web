-- REQ-0048: articulo.clasificacion pasa de texto libre a lista de catalogo (entidad).
-- Corre despues de V28 (RLS): se fija app.tenant=-1 (SET LOCAL) para insertar en entidad.
SELECT set_config('app.tenant', '-1', true);

-- Lista CLASIFICACION_ARTICULO con opciones globales (tenant -1). Opciones neutrales de arranque;
-- cada empresa agrega/edita las suyas en "Listas del sistema".
INSERT INTO entidad (lista, codigo, descripcion, tenant, estado, usuario_creacion, fecha_creacion)
  SELECT 'CLASIFICACION_ARTICULO', v.cod, v.des, -1, 'ACTIVO', 'sistema', now()
  FROM (VALUES ('GENERAL','General'), ('SERVICIO','Servicio'), ('GASTO','Gasto'), ('OTRO','Otro')) AS v(cod, des)
  WHERE NOT EXISTS (
      SELECT 1 FROM entidad e WHERE e.lista='CLASIFICACION_ARTICULO' AND e.codigo=v.cod AND e.tenant=-1);

-- Convertir articulo.clasificacion de varchar(20) libre a id de entidad (bigint).
-- No hay datos cargados ni dependencias de vista sobre la columna -> conversion directa (USING NULL).
ALTER TABLE articulo ALTER COLUMN clasificacion TYPE bigint USING NULL;
