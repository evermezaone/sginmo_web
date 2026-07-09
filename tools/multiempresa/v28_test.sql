-- ============================================================================
-- PRUEBA RLS (V28) sobre V26+V27+V28 — asserts DUROS (obs 256).
-- Correr entre BEGIN ... ROLLBACK, conectado como el rol de la app (sginmo: no superuser,
-- no bypassrls; V28 usa FORCE RLS -> aplica al owner). Cualquier desvio -> RAISE EXCEPTION,
-- por lo que psql -v ON_ERROR_STOP=1 termina EXIT != 0. Los conteos se filtran por el marcador
-- de nombre 'ACT-RLS%' para NO depender de los activos pre-existentes de la BD.
-- ============================================================================
\echo '=== PRUEBA RLS con asserts duros (tx, ROLLBACK) ==='
DO $$
DECLARE v_ent bigint; v_cnt int;
BEGIN
  -- 2do tenant (empresa 500): persona/persona_juridica NO estan bajo RLS (identidad).
  INSERT INTO persona (persona,tipo_personeria,nombre,numero_documento,estado,usuario_creacion,fecha_creacion)
    VALUES (500,'JURIDICA','EMP2','EMP2DOC','ACTIVO','t',now());
  INSERT INTO persona_juridica (persona,razon_social,usuario_creacion,fecha_creacion)
    VALUES (500,'EMP2 SA','t',now());
  SELECT entidad INTO v_ent FROM entidad WHERE lista='TIPOS_ACTIVO' AND tenant=-1 LIMIT 1;
  IF v_ent IS NULL THEN RAISE EXCEPTION 'SETUP FALLA: no hay TIPOS_ACTIVO global (-1)'; END IF;

  -- Como tenant 1: crea un activo propio (marcado).
  PERFORM set_config('app.tenant','1',true);
  INSERT INTO activo (tenant,nombre,tipo,estado,usuario_creacion,fecha_creacion)
    VALUES (1,'ACT-RLS-T1',v_ent,'LIBRE','t',now());

  -- T-write: tenant 1 intenta crear activo de tenant 500 -> RLS DEBE negarlo (WITH CHECK).
  BEGIN
    INSERT INTO activo (tenant,nombre,tipo,estado,usuario_creacion,fecha_creacion)
      VALUES (500,'ACT-RLS-ILEGAL',v_ent,'LIBRE','t',now());
    RAISE EXCEPTION 'T-write FALLA: RLS permitio a tenant 1 insertar un activo de tenant 500';
  EXCEPTION
    WHEN insufficient_privilege OR check_violation THEN NULL;   -- esperado
  END;

  -- Como tenant 500: crea su propio activo (marcado).
  PERFORM set_config('app.tenant','500',true);
  INSERT INTO activo (tenant,nombre,tipo,estado,usuario_creacion,fecha_creacion)
    VALUES (500,'ACT-RLS-T500',v_ent,'LIBRE','t',now());

  -- T-read t500: ve SOLO su activo marcado, no el de tenant 1.
  SELECT count(*) INTO v_cnt FROM activo WHERE nombre LIKE 'ACT-RLS%';
  IF v_cnt <> 1 THEN RAISE EXCEPTION 'T-read t500 FALLA: ve % activos marcados (esperado 1)', v_cnt; END IF;
  IF EXISTS (SELECT 1 FROM activo WHERE nombre='ACT-RLS-T1') THEN
    RAISE EXCEPTION 'T-read t500 FALLA: ve el activo de tenant 1'; END IF;

  -- T-read t1: ve SOLO el suyo.
  PERFORM set_config('app.tenant','1',true);
  SELECT count(*) INTO v_cnt FROM activo WHERE nombre LIKE 'ACT-RLS%';
  IF v_cnt <> 1 THEN RAISE EXCEPTION 'T-read t1 FALLA: ve % activos marcados (esperado 1)', v_cnt; END IF;
  IF EXISTS (SELECT 1 FROM activo WHERE nombre='ACT-RLS-T500') THEN
    RAISE EXCEPTION 'T-read t1 FALLA: ve el activo de tenant 500'; END IF;

  -- SUPERADMIN (-1): ve los DOS activos marcados.
  PERFORM set_config('app.tenant','-1',true);
  SELECT count(*) INTO v_cnt FROM activo WHERE nombre LIKE 'ACT-RLS%';
  IF v_cnt <> 2 THEN RAISE EXCEPTION 'T-read superadmin FALLA: ve % activos marcados (esperado 2)', v_cnt; END IF;

  -- Catalogo: tenant 1 ve los globales (-1) en entidad (al menos el TIPOS_ACTIVO usado).
  PERFORM set_config('app.tenant','1',true);
  SELECT count(*) INTO v_cnt FROM entidad WHERE tenant=-1;
  IF v_cnt = 0 THEN RAISE EXCEPTION 'T-cat FALLA: tenant 1 no ve catalogos globales (-1)'; END IF;

  -- Sin app.tenant seteado -> fail-closed: no ve NINGUN activo marcado.
  PERFORM set_config('app.tenant','',true);
  SELECT count(*) INTO v_cnt FROM activo WHERE nombre LIKE 'ACT-RLS%';
  IF v_cnt <> 0 THEN RAISE EXCEPTION 'T-unset FALLA: sin app.tenant ve % activos (esperado 0, fail-closed)', v_cnt; END IF;

  RAISE NOTICE 'v28_test OK: aislamiento read/write, superadmin total, catalogo global, fail-closed';
END $$;
