-- REQ-0043: "Nacionalidad" pasa de texto libre a lista de catalogo (tabla entidad),
-- igual que Estado civil. Corre DESPUES de V28 (RLS): se fija app.tenant=-1 (SET LOCAL) para
-- poder insertar/actualizar tablas con RLS activa (entidad, persona_empresa) durante la migracion.
SELECT set_config('app.tenant', '-1', true);

-- Lista NACIONALIDADES con opciones globales (tenant -1). Gentilicios (paraguaya, argentina...).
INSERT INTO entidad (lista, codigo, descripcion, tenant, estado, usuario_creacion, fecha_creacion)
  SELECT 'NACIONALIDADES', v.cod, v.des, -1, 'ACTIVO', 'sistema', now()
  FROM (VALUES
      ('PARAGUAYA','Paraguaya'), ('ARGENTINA','Argentina'), ('BRASILENA','Brasilena'),
      ('URUGUAYA','Uruguaya'),  ('BOLIVIANA','Boliviana'), ('CHILENA','Chilena'),
      ('PERUANA','Peruana'),    ('COLOMBIANA','Colombiana'), ('VENEZOLANA','Venezolana'),
      ('ESPANOLA','Espanola'),  ('ITALIANA','Italiana'),   ('ALEMANA','Alemana'),
      ('ESTADOUNIDENSE','Estadounidense'), ('OTRA','Otra')
    ) AS v(cod, des)
  WHERE NOT EXISTS (
      SELECT 1 FROM entidad e WHERE e.lista='NACIONALIDADES' AND e.codigo=v.cod AND e.tenant=-1);

-- Convertir persona_empresa.nacionalidad de varchar(80) libre a id de entidad (bigint).
-- La vista v_persona referencia la columna, asi que se dropea, se convierte y se recrea identica.
-- Backfill: mapea el texto existente a la opcion por descripcion (case-insensitive); lo que no
-- matchea queda NULL (dato libre no catalogado, se re-selecciona desde el combo).
DROP VIEW IF EXISTS v_persona;

ALTER TABLE persona_empresa ADD COLUMN IF NOT EXISTS nacionalidad_id bigint;
UPDATE persona_empresa pe
  SET nacionalidad_id = e.entidad
  FROM entidad e
  WHERE e.lista='NACIONALIDADES' AND e.tenant=-1
    AND pe.nacionalidad IS NOT NULL
    AND lower(trim(pe.nacionalidad)) = lower(e.descripcion);
ALTER TABLE persona_empresa DROP COLUMN nacionalidad;
ALTER TABLE persona_empresa RENAME COLUMN nacionalidad_id TO nacionalidad;

-- Recrear v_persona identica a V26 (ahora pe.nacionalidad es bigint = id de entidad).
CREATE VIEW v_persona AS
SELECT p.persona, p.tipo_personeria, p.nombre, p.numero_documento, p.digito_verificador,
       p.tipo_documento, p.estado,
       pe.tenant, pe.es_contribuyente, pe.clasificacion_fiscal, pe.direccion, pe.telefono,
       pe.email, pe.ubicacion, pe.ubicacion_url, pe.observacion, pe.estado_civil,
       pe.nacionalidad, pe.nombre_fantasia, pe.representante_legal, pe.actividad,
       pf.nombres, pf.apellidos, pf.sexo, pf.fecha_nacimiento,
       pj.razon_social, pj.fecha_constitucion
  FROM persona p
  LEFT JOIN persona_fisica   pf ON pf.persona = p.persona
  LEFT JOIN persona_juridica pj ON pj.persona = p.persona
  LEFT JOIN persona_empresa  pe ON pe.persona = p.persona;
