-- ============================================================================
-- V28 — Row-Level Security multiempresa (F5, REQ-0037)
-- Defensa en profundidad a nivel BD: aunque un service olvide filtrar, la BD no
-- deja ver/tocar datos de otro tenant. La app setea el tenant por transaccion con
-- SELECT set_config('app.tenant', <tenant>, true) (interceptor, F5b).
--
-- Regla (doc 14 §8):
--   * app_tenant() = NULLIF(current_setting('app.tenant', true), '')::bigint  (NULL si no seteado)
--   * SELECT: se ve el registro GLOBAL (-1) + el del tenant; SUPERADMIN (app_tenant=-1) ve TODO.
--   * INSERT/UPDATE/DELETE: un tenant solo escribe SUS filas (nunca -1 ni de otros);
--     SUPERADMIN (app_tenant=-1) escribe cualquiera (incluidos los -1).
--   * Si app.tenant no esta seteado (app_tenant() NULL) -> fail-closed (no ve nada).
--
-- Alcance: SOLO tablas de NEGOCIO con columna tenant. Se EXCLUYEN las tablas de
-- seguridad (usuario, grupo, usuario_grupo, permiso_*, preferencia_usuario): el login
-- las lee ANTES de haber tenant y su aislamiento por tenant es de la capa app (F6).
-- persona/persona_fisica/persona_juridica NO tienen tenant (identidad global) -> sin RLS.
-- FORCE ROW LEVEL SECURITY para que aplique tambien al owner (la app corre como el owner).
--
-- Va como unidad desplegable con V26/V27 (staging). Requiere el interceptor (F5b) para
-- que la app funcione tras aplicarla.
-- ============================================================================

CREATE OR REPLACE FUNCTION public.app_tenant() RETURNS bigint
 LANGUAGE sql STABLE
AS $$ SELECT NULLIF(current_setting('app.tenant', true), '')::bigint $$;

DO $$
DECLARE
  v_tabla text;
  v_tablas text[] := ARRAY[
    'entidad','persona_empresa','persona_rol','moneda','impuesto','forma_pago',
    'ubicacion_geografica','articulo','atributo','atributo_por_tipo','parametro_sistema',
    'sucursal','activo','operacion','planilla','cobro','ingreso_egreso','anulacion',
    'rango_comprobante','documento'
  ];
BEGIN
  FOREACH v_tabla IN ARRAY v_tablas LOOP
    EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY', v_tabla);
    EXECUTE format('ALTER TABLE public.%I FORCE ROW LEVEL SECURITY', v_tabla);

    EXECUTE format($f$
      CREATE POLICY p_%1$s_sel ON public.%1$s FOR SELECT
        USING (tenant = -1 OR tenant = app_tenant() OR app_tenant() = -1)
    $f$, v_tabla);

    EXECUTE format($f$
      CREATE POLICY p_%1$s_ins ON public.%1$s FOR INSERT
        WITH CHECK (app_tenant() = -1 OR tenant = app_tenant())
    $f$, v_tabla);

    EXECUTE format($f$
      CREATE POLICY p_%1$s_upd ON public.%1$s FOR UPDATE
        USING (app_tenant() = -1 OR tenant = app_tenant())
        WITH CHECK (app_tenant() = -1 OR tenant = app_tenant())
    $f$, v_tabla);

    EXECUTE format($f$
      CREATE POLICY p_%1$s_del ON public.%1$s FOR DELETE
        USING (app_tenant() = -1 OR tenant = app_tenant())
    $f$, v_tabla);
  END LOOP;
END $$;
