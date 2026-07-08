-- ============================================================================
-- SGInmo Web — V25: anulacion de cobro con motivo obligatorio (REQ-0023 obs 227)
-- Fiel a Gestion (P_ANULARCOBRO registra ANULACIONES con motivo obligatorio,
-- doc 10 / doc 09): f_anular_cobro exige el motivo (lista MOTIVOS_ANULACION) e
-- inserta la fila de anulacion(empresa, cobro, motivo, usuario, fecha) DENTRO de
-- la misma transaccion que la reversa contable.
-- ============================================================================

DROP FUNCTION IF EXISTS f_anular_cobro(bigint, varchar);

CREATE OR REPLACE FUNCTION f_anular_cobro(p_cobro bigint, p_usuario varchar,
                                          p_motivo varchar DEFAULT NULL)
RETURNS void AS $$
DECLARE
  r_det record; v_empresa bigint;
BEGIN
  -- motivo obligatorio; el FK (motivo_lista, motivo_codigo) valida que exista en MOTIVOS_ANULACION
  IF COALESCE(p_motivo, '') = '' THEN
    RAISE EXCEPTION 'El motivo de la anulación es obligatorio';
  END IF;

  SELECT empresa INTO v_empresa FROM cobro WHERE cobro = p_cobro AND estado = 'ACTIVO' FOR UPDATE;
  IF v_empresa IS NULL THEN
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

  -- registro auditable de la anulacion, en la MISMA transaccion que la reversa
  INSERT INTO anulacion (empresa, cobro, motivo_codigo, fecha, usuario_creacion, fecha_creacion)
    VALUES (v_empresa, p_cobro, p_motivo, now(), p_usuario, now());
END;
$$ LANGUAGE plpgsql;
