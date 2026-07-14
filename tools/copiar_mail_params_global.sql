-- ============================================================================
-- Copia los parametros del relay de correo (MAIL_HTTP_URL / MAIL_HTTP_TOKEN)
-- que fueron cargados bajo una EMPRESA hacia el default GLOBAL (tenant = -1).
--
-- Motivo: CorreoService lee los parametros via IntegracionSeguridad.valor(),
-- que consulta EXCLUSIVAMENTE "tenant = -1". Un parametro cargado en una empresa
-- (override) no es visto por el envio de correo (OTP del portal, etc.).
--
-- Seguro de re-ejecutar: ON CONFLICT (tenant, clave) DO UPDATE.
-- Requiere contexto SUPERADMIN (app.tenant = -1) para pasar RLS (V28):
--   * SELECT con app_tenant()=-1 ve TODAS las empresas (lee el origen).
--   * INSERT/UPDATE con app_tenant()=-1 puede escribir el registro -1.
-- ============================================================================

SELECT set_config('app.tenant', '-1', false);  -- contexto SUPERADMIN (RLS V28)

DO $$
DECLARE
  cols  text;   -- lista de columnas destino (todas, en orden fisico)
  selcs text;   -- lista SELECT del origen, con tenant forzado a -1
  updcs text;   -- SET del UPDATE en el ON CONFLICT (todo menos la PK)
  k     text;
  src   bigint;
  n     int;
BEGIN
  SELECT string_agg(quote_ident(column_name), ', ' ORDER BY ordinal_position),
         string_agg(CASE WHEN column_name = 'tenant' THEN '-1::bigint'
                         ELSE 'p.' || quote_ident(column_name) END, ', ' ORDER BY ordinal_position),
         string_agg(CASE WHEN column_name NOT IN ('tenant','clave')
                         THEN quote_ident(column_name) || ' = EXCLUDED.' || quote_ident(column_name)
                    END, ', ')
    INTO cols, selcs, updcs
  FROM information_schema.columns
  WHERE table_schema = 'public' AND table_name = 'parametro_sistema';

  FOREACH k IN ARRAY ARRAY['MAIL_HTTP_URL','MAIL_HTTP_TOKEN'] LOOP
    SELECT count(*), max(tenant) INTO n, src
      FROM parametro_sistema WHERE clave = k AND tenant <> -1;

    IF n = 0 THEN
      RAISE NOTICE '[%] sin origen en ninguna empresa: se omite', k;
      CONTINUE;
    END IF;
    IF n > 1 THEN
      RAISE NOTICE '[%] hay % origenes distintos; se copia el del tenant % (el mayor)', k, n, src;
    END IF;

    EXECUTE format(
      'INSERT INTO parametro_sistema (%s) SELECT %s FROM parametro_sistema p '
      || 'WHERE p.clave = %L AND p.tenant = %s '
      || 'ON CONFLICT (tenant, clave) DO UPDATE SET %s',
      cols, selcs, k, src, updcs);

    RAISE NOTICE '[%] copiado a tenant -1 (origen: tenant %)', k, src;
  END LOOP;
END $$;

-- Verificacion (el token se muestra SOLO como longitud, nunca el valor).
SELECT clave,
       tenant,
       CASE WHEN clave LIKE '%TOKEN%' THEN '(oculto, len ' || length(valor) || ')'
            ELSE valor END AS valor
  FROM parametro_sistema
 WHERE clave IN ('MAIL_HTTP_URL','MAIL_HTTP_TOKEN')
 ORDER BY clave, tenant;
