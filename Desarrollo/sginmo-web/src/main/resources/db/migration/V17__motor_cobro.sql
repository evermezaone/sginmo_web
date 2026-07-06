-- V17 (REQ-0022/0023): motor de cobros EN LA BASE (doc 10). Java invoca estos SPs.

-- ── Calculo de mora de una cuota a una fecha ──
-- mora = dias_atraso * monto_mora_diario (parametro de la operacion), tras los dias de gracia.
CREATE OR REPLACE FUNCTION f_mora_cuota(p_cuota bigint, p_fecha date)
RETURNS numeric(15,2) AS $$
DECLARE
  v_venc date; v_saldo numeric; v_operacion bigint;
  v_monto_mora numeric; v_gracia int; v_dias int;
BEGIN
  SELECT c.fecha_vencimiento, c.saldo, c.operacion INTO v_venc, v_saldo, v_operacion
    FROM cronograma_cuota c WHERE c.cronograma_cuota = p_cuota;
  IF v_venc IS NULL OR v_saldo <= 0 THEN RETURN 0; END IF;
  SELECT COALESCE(o.monto_mora,0), COALESCE(o.dias_gracia,0) INTO v_monto_mora, v_gracia
    FROM operacion o WHERE o.operacion = v_operacion;
  v_dias := (p_fecha - v_venc) - v_gracia;
  IF v_dias <= 0 OR v_monto_mora <= 0 THEN RETURN 0; END IF;
  RETURN round(v_dias * v_monto_mora, 2);
END;
$$ LANGUAGE plpgsql;

-- ── Cobrar: aplica un monto a un documento y cancela cuotas afectadas (FIFO por vencimiento) ──
-- Registra cobro + cobro_detalle, baja el saldo del documento (el trigger recalcula estado),
-- y cancela cuotas del cronograma en orden de vencimiento. Devuelve el id del cobro.
CREATE OR REPLACE FUNCTION f_cobrar_documento(
  p_documento bigint, p_planilla bigint, p_forma_pago bigint, p_persona bigint,
  p_monto numeric, p_moneda bigint, p_fecha date, p_usuario varchar)
RETURNS bigint AS $$
DECLARE
  v_saldo numeric; v_empresa bigint; v_sucursal bigint; v_cobro bigint;
  v_aplicar numeric; v_operacion bigint; r_cuota record; v_pago numeric;
BEGIN
  IF p_monto <= 0 THEN RAISE EXCEPTION 'El monto a cobrar debe ser positivo'; END IF;
  SELECT saldo, empresa, sucursal INTO v_saldo, v_empresa, v_sucursal
    FROM documento WHERE documento = p_documento AND estado <> 'ANULADO' FOR UPDATE;
  IF v_saldo IS NULL THEN RAISE EXCEPTION 'Documento inexistente o anulado'; END IF;
  IF p_monto > v_saldo THEN
    RAISE EXCEPTION 'El monto (%) supera el saldo del documento (%)', p_monto, v_saldo;
  END IF;

  INSERT INTO cobro (empresa, sucursal, planilla, forma_pago, persona, cajero, fecha, hora, monto,
                     moneda, usuario_creacion, fecha_creacion)
    VALUES (v_empresa, v_sucursal, p_planilla, p_forma_pago, p_persona, p_usuario, p_fecha, now(),
            p_monto, p_moneda, p_usuario, now())
    RETURNING cobro INTO v_cobro;

  INSERT INTO cobro_detalle (cobro, secuencia, documento, monto, usuario_creacion, fecha_creacion)
    VALUES (v_cobro, 1, p_documento, p_monto, p_usuario, now());

  -- baja el saldo del documento (trigger td_documento_estado deriva el estado)
  UPDATE documento SET saldo = saldo - p_monto, version = version + 1 WHERE documento = p_documento;

  -- suma al monto cobrado de la planilla (caja)
  UPDATE planilla SET monto_cobro = monto_cobro + p_monto, version = version + 1
    WHERE planilla = p_planilla;

  -- cancela cuotas del cronograma afectadas (FIFO por vencimiento)
  v_aplicar := p_monto;
  FOR r_cuota IN
    SELECT c.cronograma_cuota, c.saldo FROM cronograma_cuota c
      WHERE c.documento = p_documento AND c.estado = 'PENDIENTE'
      ORDER BY c.fecha_vencimiento
      FOR UPDATE
  LOOP
    EXIT WHEN v_aplicar <= 0;
    v_pago := LEAST(v_aplicar, r_cuota.saldo);
    UPDATE cronograma_cuota
      SET saldo = saldo - v_pago,
          estado = CASE WHEN saldo - v_pago <= 0 THEN 'CANCELADO' ELSE 'PENDIENTE' END,
          fecha_cancelacion = CASE WHEN saldo - v_pago <= 0 THEN p_fecha ELSE fecha_cancelacion END,
          version = version + 1
      WHERE cronograma_cuota = r_cuota.cronograma_cuota;
    v_aplicar := v_aplicar - v_pago;
  END LOOP;

  RETURN v_cobro;
END;
$$ LANGUAGE plpgsql;

-- ── Anular un cobro: revierte saldo del documento, cuotas y caja (REQ-0023) ──
CREATE OR REPLACE FUNCTION f_anular_cobro(p_cobro bigint, p_usuario varchar) RETURNS void AS $$
DECLARE
  r_det record; v_reponer numeric; r_cuota record; v_dev numeric;
BEGIN
  IF NOT EXISTS (SELECT 1 FROM cobro WHERE cobro = p_cobro AND estado = 'ACTIVO' FOR UPDATE) THEN
    RAISE EXCEPTION 'El cobro no existe o ya esta anulado';
  END IF;

  FOR r_det IN SELECT documento, monto FROM cobro_detalle WHERE cobro = p_cobro AND estado = 'ACTIVO' LOOP
    -- repone el saldo del documento (el trigger recalcula estado a PENDIENTE)
    UPDATE documento SET saldo = LEAST(saldo + r_det.monto, total), version = version + 1
      WHERE documento = r_det.documento;
    -- reabre cuotas afectadas en orden inverso (ultima cancelada primero)
    v_reponer := r_det.monto;
    FOR r_cuota IN
      SELECT cronograma_cuota, monto, saldo FROM cronograma_cuota
        WHERE documento = r_det.documento
        ORDER BY fecha_vencimiento DESC FOR UPDATE
    LOOP
      EXIT WHEN v_reponer <= 0;
      v_dev := LEAST(v_reponer, r_cuota.monto - r_cuota.saldo);
      IF v_dev > 0 THEN
        UPDATE cronograma_cuota
          SET saldo = saldo + v_dev, estado = 'PENDIENTE', fecha_cancelacion = NULL, version = version + 1
          WHERE cronograma_cuota = r_cuota.cronograma_cuota;
        v_reponer := v_reponer - v_dev;
      END IF;
    END LOOP;
  END LOOP;

  -- descuenta de la caja
  UPDATE planilla p SET monto_cobro = p.monto_cobro - c.monto, version = p.version + 1
    FROM cobro c WHERE c.cobro = p_cobro AND p.planilla = c.planilla;

  UPDATE cobro_detalle SET estado = 'ANULADO', usuario_modificacion = p_usuario, fecha_modificacion = now()
    WHERE cobro = p_cobro;
  UPDATE cobro SET estado = 'ANULADO', usuario_modificacion = p_usuario, fecha_modificacion = now()
    WHERE cobro = p_cobro;
END;
$$ LANGUAGE plpgsql;
