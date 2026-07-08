-- ============================================================================
-- SGInmo Web — V23: endurecimiento del cobro (REQ-0022, obs 224/225 de Codex)
-- 1) f_cobrar_documento valida la planilla (FOR UPDATE, ABIERTA, misma
--    empresa/sucursal del documento) antes de registrar el cobro.
-- 2) La forma de pago parametriza los datos exigibles (flags requiere_*, doc 10):
--    se validan en el SP y se persiste dato_cobro junto al cobro.
-- f_cobrar_total sigue funcionando: los parametros nuevos tienen DEFAULT NULL
-- (efectivo-sin-datos, la unica forma valida para el cobro en cascada).
-- ============================================================================

-- Semillas minimas de las listas configurables de datos de cobro (el admin amplia)
INSERT INTO entidad (entidad, codigo, descripcion, usuario_creacion, fecha_creacion) VALUES
 ('EMISORES','OTRO','Otro emisor','sistema',now()),
 ('PROCESADORES','OTRO','Otro procesador','sistema',now())
ON CONFLICT (entidad, codigo) DO NOTHING;

DROP FUNCTION IF EXISTS f_cobrar_documento(bigint,bigint,bigint,bigint,numeric,bigint,date,varchar);

CREATE OR REPLACE FUNCTION f_cobrar_documento(
  p_documento bigint, p_planilla bigint, p_forma_pago bigint, p_persona bigint,
  p_monto numeric, p_moneda bigint, p_fecha date, p_usuario varchar,
  p_emisor varchar DEFAULT NULL, p_procesador varchar DEFAULT NULL,
  p_numero varchar DEFAULT NULL, p_serie varchar DEFAULT NULL,
  p_cuenta varchar DEFAULT NULL, p_vencimiento date DEFAULT NULL,
  p_referencia varchar DEFAULT NULL)
RETURNS bigint AS $$
DECLARE
  v_saldo numeric; v_empresa bigint; v_sucursal bigint; v_cobro bigint;
  v_pla_empresa bigint; v_pla_sucursal bigint; v_pla_estado varchar;
  v_fp forma_pago%ROWTYPE;
BEGIN
  -- obs 224: la planilla debe existir (FOR UPDATE), estar ABIERTA y coincidir
  -- en empresa/sucursal con el documento que se cobra.
  SELECT empresa, sucursal, estado INTO v_pla_empresa, v_pla_sucursal, v_pla_estado
    FROM planilla WHERE planilla = p_planilla FOR UPDATE;
  IF v_pla_estado IS NULL THEN RAISE EXCEPTION 'La planilla de caja no existe'; END IF;
  IF v_pla_estado <> 'ABIERTA' THEN RAISE EXCEPTION 'La planilla de caja no está abierta'; END IF;

  -- validaciones en el mismo orden que P_PAGARCOMPROBANTE modo 'C'
  SELECT saldo, empresa, sucursal INTO v_saldo, v_empresa, v_sucursal
    FROM documento WHERE documento = p_documento AND estado <> 'ANULADO' FOR UPDATE;
  IF v_saldo IS NULL THEN RAISE EXCEPTION 'Documento inexistente o anulado'; END IF;
  IF v_saldo <= 0 THEN RAISE EXCEPTION 'El saldo del comprobante es 0 o menor a cero'; END IF;
  IF p_monto <= 0 THEN RAISE EXCEPTION 'El monto debe ser mayor a cero'; END IF;
  IF v_saldo < p_monto THEN RAISE EXCEPTION 'El monto que desea pagar es mayor al saldo de esta factura'; END IF;
  IF v_empresa <> v_pla_empresa OR v_sucursal <> v_pla_sucursal THEN
    RAISE EXCEPTION 'La planilla de caja pertenece a otra empresa/sucursal';
  END IF;

  -- obs 225: datos exigibles parametrizados por la forma de pago (doc 10).
  -- p_forma_pago NULL = efectivo sin especificar (sin flags que exigir).
  IF p_forma_pago IS NOT NULL THEN
    SELECT * INTO v_fp FROM forma_pago WHERE forma_pago = p_forma_pago;
    IF v_fp.forma_pago IS NULL THEN RAISE EXCEPTION 'Forma de pago inexistente'; END IF;
    IF v_fp.requiere_emisor      AND COALESCE(p_emisor, '')     = '' THEN RAISE EXCEPTION '% exige indicar el emisor',       v_fp.descripcion; END IF;
    IF v_fp.requiere_procesador  AND COALESCE(p_procesador, '') = '' THEN RAISE EXCEPTION '% exige indicar el procesador',   v_fp.descripcion; END IF;
    IF v_fp.requiere_numero      AND COALESCE(p_numero, '')     = '' THEN RAISE EXCEPTION '% exige indicar el número',       v_fp.descripcion; END IF;
    IF v_fp.requiere_serie       AND COALESCE(p_serie, '')      = '' THEN RAISE EXCEPTION '% exige indicar la serie',        v_fp.descripcion; END IF;
    IF v_fp.requiere_vencimiento AND p_vencimiento IS NULL           THEN RAISE EXCEPTION '% exige la fecha de vencimiento', v_fp.descripcion; END IF;
    IF v_fp.requiere_cuenta      AND COALESCE(p_cuenta, '')     = '' THEN RAISE EXCEPTION '% exige indicar la cuenta',       v_fp.descripcion; END IF;
    IF v_fp.requiere_referencia  AND COALESCE(p_referencia, '') = '' THEN RAISE EXCEPTION '% exige indicar la referencia',   v_fp.descripcion; END IF;
  END IF;

  INSERT INTO cobro (empresa, sucursal, planilla, forma_pago, persona, cajero, fecha, hora, monto,
                     moneda, usuario_creacion, fecha_creacion)
    VALUES (v_empresa, v_sucursal, p_planilla, p_forma_pago, p_persona, p_usuario, p_fecha, now(),
            p_monto, p_moneda, p_usuario, now())
    RETURNING cobro INTO v_cobro;

  INSERT INTO cobro_detalle (cobro, secuencia, documento, monto, usuario_creacion, fecha_creacion)
    VALUES (v_cobro, 1, p_documento, p_monto, p_usuario, now());

  -- dato_cobro junto al cobro cuando el medio de pago trae datos (obs 225)
  IF COALESCE(p_emisor,'') <> '' OR COALESCE(p_procesador,'') <> '' OR COALESCE(p_numero,'') <> ''
     OR COALESCE(p_serie,'') <> '' OR COALESCE(p_cuenta,'') <> '' OR p_vencimiento IS NOT NULL
     OR COALESCE(p_referencia,'') <> '' THEN
    INSERT INTO dato_cobro (cobro, emisor_codigo, procesador_codigo, numero, serie,
                            cuenta_corriente, fecha_vencimiento, referencia,
                            usuario_creacion, fecha_creacion)
      VALUES (v_cobro, NULLIF(p_emisor,''), NULLIF(p_procesador,''), NULLIF(p_numero,''),
              NULLIF(p_serie,''), NULLIF(p_cuenta,''), p_vencimiento, NULLIF(p_referencia,''),
              p_usuario, now());
  END IF;

  -- baja el saldo del documento (trigger td_documento_estado deriva el estado PENDIENTE/CANCELADO)
  UPDATE documento SET saldo = saldo - p_monto, version = version + 1 WHERE documento = p_documento;

  -- suma a la caja
  UPDATE planilla SET monto_cobro = monto_cobro + p_monto, version = version + 1 WHERE planilla = p_planilla;

  -- recalcula las cuotas desde el total pagado (Gestion: p_actualizasaldocuotas)
  PERFORM f_actualiza_saldo_cuotas(p_documento);

  RETURN v_cobro;
END;
$$ LANGUAGE plpgsql;
