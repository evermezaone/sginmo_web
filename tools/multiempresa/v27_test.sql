\echo '=== PRUEBA FUNCIONAL MOTOR sobre V26+V27 (tx, ROLLBACK) ==='
DO $$
DECLARE
  v_rango bigint; v_doc bigint; v_pla bigint; v_num bigint; v_cobro bigint;
  v_ct bigint; v_an record; v_saldo numeric; v_caja numeric;
BEGIN
  INSERT INTO rango_comprobante (tenant, tipo, serie, numero_desde, numero_actual, numero_hasta, estado, usuario_creacion, fecha_creacion)
    VALUES (1, 'REC', 'T1', 1, 1, 100, 'ACTIVO', 'test', now()) RETURNING rango_comprobante INTO v_rango;

  SELECT f_siguiente_numero(1, 'REC', 'T1') INTO v_num;
  RAISE NOTICE 'T1 f_siguiente_numero=% (esperado 1)', v_num;

  INSERT INTO documento (tenant, empresa, tipo, serie, numero, fecha, persona, sucursal, moneda, cotizacion, total, saldo, direccion_dinero, estado, usuario_creacion, fecha_creacion)
    VALUES (1, 1, 'REC', 'T1', v_num, current_date, 1, 1, 1, 1, 100000, 100000, 'ENTRADA', 'PENDIENTE', 'test', now()) RETURNING documento INTO v_doc;
  INSERT INTO documento_detalle (documento, numero_item, concepto, cantidad, precio_unitario, monto, saldo, usuario_creacion, fecha_creacion)
    VALUES (v_doc, 1, 'test', 1, 100000, 100000, 100000, 'test', now());
  INSERT INTO planilla (tenant, sucursal, usuario_apertura, fecha_apertura, hora_apertura, monto_apertura, monto_cobro, estado, usuario_creacion, fecha_creacion)
    VALUES (1, 1, 'test', current_date, now(), 0, 0, 'ABIERTA', 'test', now()) RETURNING planilla INTO v_pla;

  SELECT f_cobrar_documento(v_doc, v_pla, NULL, 1, 70000, 1, current_date, 'test') INTO v_cobro;
  SELECT tenant INTO v_ct FROM cobro WHERE cobro = v_cobro;
  SELECT saldo INTO v_saldo FROM documento WHERE documento = v_doc;
  SELECT monto_cobro INTO v_caja FROM planilla WHERE planilla = v_pla;
  RAISE NOTICE 'T2 cobro=% tenant=% (esp 1) saldo_doc=% (esp 30000) caja=% (esp 70000)', v_cobro, v_ct, v_saldo, v_caja;

  -- T3: motivo inexistente -> rechazo (cobro aun ACTIVO)
  BEGIN
    PERFORM f_anular_cobro(v_cobro, 'test', 'NO_EXISTE_XX');
    RAISE NOTICE 'T3 FALLO: acepto motivo inexistente';
  EXCEPTION WHEN OTHERS THEN RAISE NOTICE 'T3 OK motivo inexistente rechazado: %', SQLERRM; END;

  -- T4: anular con motivo valido -> repone saldo, baja caja, registra anulacion con motivo por ID
  PERFORM f_anular_cobro(v_cobro, 'test', 'ERROR_CARGA');
  SELECT tenant, cobro, motivo INTO v_an FROM anulacion WHERE cobro = v_cobro;
  SELECT saldo INTO v_saldo FROM documento WHERE documento = v_doc;
  SELECT monto_cobro INTO v_caja FROM planilla WHERE planilla = v_pla;
  RAISE NOTICE 'T4 OK anulacion tenant=% cobro=% motivo_id=% | saldo_repuesto=% (esp 100000) caja=% (esp 0)',
    v_an.tenant, v_an.cobro, v_an.motivo, v_saldo, v_caja;
END $$;
