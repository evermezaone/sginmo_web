\echo '=== PRUEBA RLS sobre V26+V27+V28 (tx, ROLLBACK) ==='
DO $$
DECLARE v_ent bigint; v_cnt int;
BEGIN
  -- 2do tenant (empresa 500)
  INSERT INTO persona (persona,tipo_personeria,nombre,numero_documento,estado,usuario_creacion,fecha_creacion)
    VALUES (500,'JURIDICA','EMP2','EMP2DOC','ACTIVO','t',now());
  INSERT INTO persona_juridica (persona,razon_social,usuario_creacion,fecha_creacion)
    VALUES (500,'EMP2 SA','t',now());
  SELECT entidad INTO v_ent FROM entidad WHERE lista='TIPOS_ACTIVO' LIMIT 1;

  -- Como tenant 1: crea un activo propio
  PERFORM set_config('app.tenant','1',true);
  INSERT INTO activo (tenant,nombre,tipo,estado,usuario_creacion,fecha_creacion)
    VALUES (1,'ACT-T1',v_ent,'LIBRE','t',now());

  -- T-write: tenant 1 intenta crear activo de tenant 500 -> RLS lo niega
  BEGIN
    INSERT INTO activo (tenant,nombre,tipo,estado,usuario_creacion,fecha_creacion)
      VALUES (500,'ACT-T500-ilegal',v_ent,'LIBRE','t',now());
    RAISE NOTICE 'T-write FALLO: tenant 1 inserto activo de tenant 500';
  EXCEPTION WHEN OTHERS THEN RAISE NOTICE 'T-write OK: RLS nego insert cross-tenant';
  END;

  -- Como tenant 500: crea su propio activo
  PERFORM set_config('app.tenant','500',true);
  INSERT INTO activo (tenant,nombre,tipo,estado,usuario_creacion,fecha_creacion)
    VALUES (500,'ACT-T500',v_ent,'LIBRE','t',now());
  SELECT count(*) INTO v_cnt FROM activo;
  RAISE NOTICE 'T-read t500 ve % activos (esperado 1: solo el suyo)', v_cnt;

  -- Como tenant 1: ve solo el suyo
  PERFORM set_config('app.tenant','1',true);
  SELECT count(*) INTO v_cnt FROM activo;
  RAISE NOTICE 'T-read t1 ve % activos (esperado 1: solo el suyo)', v_cnt;

  -- SUPERADMIN (-1): ve todo
  PERFORM set_config('app.tenant','-1',true);
  SELECT count(*) INTO v_cnt FROM activo;
  RAISE NOTICE 'T-read superadmin ve % activos (esperado 2)', v_cnt;

  -- Catalogo: tenant 1 ve los globales (-1) en entidad
  PERFORM set_config('app.tenant','1',true);
  SELECT count(*) INTO v_cnt FROM entidad;
  RAISE NOTICE 'T-cat t1 ve % opciones de entidad (globales -1, esperado 108)', v_cnt;

  -- Sin app.tenant seteado -> fail-closed (no ve nada transaccional)
  PERFORM set_config('app.tenant','',true);
  SELECT count(*) INTO v_cnt FROM activo;
  RAISE NOTICE 'T-unset ve % activos (esperado 0: fail-closed)', v_cnt;
END $$;
