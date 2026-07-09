\echo '=== VERIFICACION V26 (dentro de tx; se hace ROLLBACK al final) ==='
SELECT 'entidad total/globales' AS chk, count(*) AS total, count(*) FILTER (WHERE tenant=-1) AS glob FROM entidad;
SELECT 'entidad PK tipo' AS chk, pg_typeof(entidad)::text AS v FROM entidad LIMIT 1;
SELECT 'pares *_lista/*_codigo restantes (debe 0)' AS chk, count(*) AS v
  FROM information_schema.columns WHERE table_schema='public'
   AND (column_name LIKE '%\_lista' OR column_name LIKE '%\_codigo')
   AND column_name NOT IN ('codigo_barra','codigo_interno','codigo_oficial','codigo_usuario');
SELECT 'columnas empresa restantes (solo documento)' AS chk, coalesce(string_agg(table_name,','),'(ninguna)') AS v
  FROM information_schema.columns WHERE table_schema='public' AND column_name='empresa';
SELECT 'ubicacion nivel nulos (debe 0)' AS chk, count(*) AS v FROM ubicacion_geografica WHERE nivel IS NULL;
SELECT 'persona_rol' AS chk, persona::text||' t='||tenant::text||' rol='||rol::text AS v FROM persona_rol ORDER BY persona,rol;
SELECT 'persona_empresa filas/tenants' AS chk, count(*)::text||' tenants='||coalesce(string_agg(distinct tenant::text,','),'-') AS v FROM persona_empresa;
SELECT 'persona cols movidas restantes (debe 0)' AS chk, count(*) AS v
  FROM information_schema.columns WHERE table_name='persona' AND column_name IN ('direccion','email','es_contribuyente','telefono','ubicacion','observacion');
SELECT 'documento tenant+empresa+tipo' AS chk, string_agg(column_name,',' ORDER BY column_name) AS v
  FROM information_schema.columns WHERE table_name='documento' AND column_name IN ('tenant','empresa','tipo');
SELECT 'parametro_sistema PK' AS chk, pg_get_constraintdef(oid) AS v FROM pg_constraint WHERE conrelid='parametro_sistema'::regclass AND contype='p';
SELECT 'usuario perfil check' AS chk, pg_get_constraintdef(oid) AS v FROM pg_constraint WHERE conrelid='usuario'::regclass AND conname='usuario_perfil_check';
SELECT 'superadmin creado' AS chk, codigo_usuario||' perfil='||perfil||' t='||tenant::text AS v FROM usuario WHERE perfil='SUPERADMIN';
SELECT 'persona GLOBAL -1' AS chk, persona::text||' '||nombre AS v FROM persona WHERE persona=-1;
SELECT 'v_persona consultable' AS chk, count(*) AS v FROM v_persona;
SELECT 'v_operacion_saldo consultable' AS chk, count(*) AS v FROM v_operacion_saldo;
SELECT 'catalogos con tenant<>-1 (debe 0)' AS chk,
  (SELECT count(*) FROM moneda WHERE tenant<>-1)+(SELECT count(*) FROM articulo WHERE tenant<>-1)
  +(SELECT count(*) FROM forma_pago WHERE tenant<>-1)+(SELECT count(*) FROM impuesto WHERE tenant<>-1) AS v;
SELECT 'FKs compuestas a entidad restantes (debe 0)' AS chk, count(*) AS v
  FROM pg_constraint WHERE confrelid='entidad'::regclass AND contype='f' AND array_length(conkey,1)>1;
SELECT 'FKs (1 col) nuevas a entidad' AS chk, count(*) AS v
  FROM pg_constraint WHERE confrelid='entidad'::regclass AND contype='f' AND array_length(conkey,1)=1;
SELECT 'articulo unique (tenant,codigo)?' AS chk, count(*) AS v FROM pg_constraint WHERE conrelid='articulo'::regclass AND conname='articulo_tenant_codigo_key';
