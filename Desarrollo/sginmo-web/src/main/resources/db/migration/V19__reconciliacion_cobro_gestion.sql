-- V19 (REQ-0022): reconciliacion del motor de cobro con la logica probada de Gestion
-- (P_PAGARCOMPROBANTE + p_actualizasaldocuotas). Se adopta el patron IDEMPOTENTE de
-- recalculo de cuotas desde el total pagado del documento (mas robusto que el FIFO
-- incremental de V17). Traduccion fiel del PL/SQL de one@XE, con las correcciones del
-- porting (sin COMMIT interno, estados PENDIENTE/CANCELADO, version optimista).

-- ── f_actualiza_saldo_cuotas: recalcula las cuotas del documento desde lo ya pagado ──
-- Traduccion de p_actualizasaldocuotas: total_pagado = total - saldo; resetea todas las
-- cuotas a PENDIENTE/saldo=monto; luego, en orden de numero_cuota, cubre cuota a cuota:
-- si total_pagado >= monto -> CANCELADO saldo 0; si parcial -> PENDIENTE saldo restante.
CREATE OR REPLACE FUNCTION f_actualiza_saldo_cuotas(p_documento bigint) RETURNS void AS $$
DECLARE
  v_total numeric(15,2);
  v_saldo numeric(15,2);
  v_pagado numeric(15,2);
  v_estado varchar;
  r record;
BEGIN
  SELECT total, saldo, estado INTO v_total, v_saldo, v_estado
    FROM documento WHERE documento = p_documento;
  IF v_total IS NULL THEN
    RETURN;   -- documento inexistente: nada que hacer
  END IF;
  IF v_estado = 'ANULADO' THEN
    RETURN;
  END IF;
  v_pagado := v_total - v_saldo;   -- lo efectivamente cobrado del documento

  -- reset de todas las cuotas del documento
  UPDATE cronograma_cuota
    SET saldo = monto, estado = 'PENDIENTE', fecha_cancelacion = NULL, version = version + 1
    WHERE documento = p_documento;

  -- cascada por numero de cuota
  FOR r IN
    SELECT cronograma_cuota, monto FROM cronograma_cuota
      WHERE documento = p_documento ORDER BY numero_cuota
  LOOP
    EXIT WHEN v_pagado <= 0;
    IF v_pagado >= r.monto THEN
      UPDATE cronograma_cuota
        SET saldo = 0, estado = 'CANCELADO', fecha_cancelacion = current_date, version = version + 1
        WHERE cronograma_cuota = r.cronograma_cuota;
      v_pagado := v_pagado - r.monto;
    ELSE
      UPDATE cronograma_cuota
        SET saldo = r.monto - v_pagado, estado = 'PENDIENTE', version = version + 1
        WHERE cronograma_cuota = r.cronograma_cuota;
      v_pagado := 0;
    END IF;
  END LOOP;
END;
$$ LANGUAGE plpgsql;

-- ── f_cobrar_documento reescrito: valida como Gestion, actualiza saldo (trigger) y
--    recalcula cuotas con f_actualiza_saldo_cuotas (patron idempotente). ──
CREATE OR REPLACE FUNCTION f_cobrar_documento(
  p_documento bigint, p_planilla bigint, p_forma_pago bigint, p_persona bigint,
  p_monto numeric, p_moneda bigint, p_fecha date, p_usuario varchar)
RETURNS bigint AS $$
DECLARE
  v_saldo numeric; v_empresa bigint; v_sucursal bigint; v_cobro bigint;
BEGIN
  -- validaciones en el mismo orden que P_PAGARCOMPROBANTE modo 'C'
  SELECT saldo, empresa, sucursal INTO v_saldo, v_empresa, v_sucursal
    FROM documento WHERE documento = p_documento AND estado <> 'ANULADO' FOR UPDATE;
  IF v_saldo IS NULL THEN RAISE EXCEPTION 'Documento inexistente o anulado'; END IF;
  IF v_saldo <= 0 THEN RAISE EXCEPTION 'El saldo del comprobante es 0 o menor a cero'; END IF;
  IF p_monto <= 0 THEN RAISE EXCEPTION 'El monto debe ser mayor a cero'; END IF;
  IF v_saldo < p_monto THEN RAISE EXCEPTION 'El monto que desea pagar es mayor al saldo de esta factura'; END IF;

  INSERT INTO cobro (empresa, sucursal, planilla, forma_pago, persona, cajero, fecha, hora, monto,
                     moneda, usuario_creacion, fecha_creacion)
    VALUES (v_empresa, v_sucursal, p_planilla, p_forma_pago, p_persona, p_usuario, p_fecha, now(),
            p_monto, p_moneda, p_usuario, now())
    RETURNING cobro INTO v_cobro;

  INSERT INTO cobro_detalle (cobro, secuencia, documento, monto, usuario_creacion, fecha_creacion)
    VALUES (v_cobro, 1, p_documento, p_monto, p_usuario, now());

  -- baja el saldo del documento (trigger td_documento_estado deriva el estado PENDIENTE/CANCELADO)
  UPDATE documento SET saldo = saldo - p_monto, version = version + 1 WHERE documento = p_documento;

  -- suma a la caja
  UPDATE planilla SET monto_cobro = monto_cobro + p_monto, version = version + 1 WHERE planilla = p_planilla;

  -- recalcula las cuotas desde el total pagado (Gestion: p_actualizasaldocuotas)
  PERFORM f_actualiza_saldo_cuotas(p_documento);

  RETURN v_cobro;
END;
$$ LANGUAGE plpgsql;

-- ── f_anular_cobro reescrito: repone saldo del documento y RECALCULA cuotas ──
-- (Gestion: TD_COBRODETALLE devuelve el saldo y luego se reajustan las cuotas).
-- Mucho mas simple y robusto que reabrir cuotas en orden inverso.
CREATE OR REPLACE FUNCTION f_anular_cobro(p_cobro bigint, p_usuario varchar) RETURNS void AS $$
DECLARE
  r_det record;
BEGIN
  IF NOT EXISTS (SELECT 1 FROM cobro WHERE cobro = p_cobro AND estado = 'ACTIVO' FOR UPDATE) THEN
    RAISE EXCEPTION 'El cobro no existe o ya esta anulado';
  END IF;

  FOR r_det IN SELECT documento, monto FROM cobro_detalle WHERE cobro = p_cobro AND estado = 'ACTIVO' LOOP
    UPDATE documento SET saldo = LEAST(saldo + r_det.monto, total), version = version + 1
      WHERE documento = r_det.documento;
    PERFORM f_actualiza_saldo_cuotas(r_det.documento);   -- recalcula tras reponer el saldo
  END LOOP;

  UPDATE planilla p SET monto_cobro = p.monto_cobro - c.monto, version = p.version + 1
    FROM cobro c WHERE c.cobro = p_cobro AND p.planilla = c.planilla;

  UPDATE cobro_detalle SET estado = 'ANULADO', usuario_modificacion = p_usuario, fecha_modificacion = now()
    WHERE cobro = p_cobro;
  UPDATE cobro SET estado = 'ANULADO', usuario_modificacion = p_usuario, fecha_modificacion = now()
    WHERE cobro = p_cobro;
END;
$$ LANGUAGE plpgsql;

-- ── f_cobrar_total: modo 'T' de Gestion (pago total en cascada FIFO por vencimiento) ──
-- Aplica p_monto sobre los documentos PENDIENTES del cliente en orden fecha/vencimiento,
-- llamando a f_cobrar_documento por cada uno hasta agotar el monto. Devuelve cuantos
-- documentos toco.
CREATE OR REPLACE FUNCTION f_cobrar_total(
  p_persona bigint, p_planilla bigint, p_forma_pago bigint, p_monto numeric,
  p_moneda bigint, p_fecha date, p_usuario varchar)
RETURNS int AS $$
DECLARE
  v_resta numeric := p_monto;
  v_aplicar numeric;
  v_tocados int := 0;
  r record;
BEGIN
  IF p_monto <= 0 THEN RAISE EXCEPTION 'El monto debe ser mayor a cero'; END IF;
  FOR r IN
    SELECT documento, saldo FROM documento
      WHERE persona = p_persona AND estado = 'PENDIENTE' AND direccion_dinero = 'ENTRADA' AND saldo > 0
      ORDER BY fecha ASC, fecha_vencimiento ASC NULLS LAST
  LOOP
    EXIT WHEN v_resta <= 0;
    v_aplicar := LEAST(v_resta, r.saldo);
    PERFORM f_cobrar_documento(r.documento, p_planilla, p_forma_pago, p_persona,
                               v_aplicar, p_moneda, p_fecha, p_usuario);
    v_resta := v_resta - v_aplicar;
    v_tocados := v_tocados + 1;
  END LOOP;
  RETURN v_tocados;
END;
$$ LANGUAGE plpgsql;
