-- ============================================================================
-- SGInmo Web — V24: cobertura COMPLETA de los flags requiere_* (REQ-0022 obs 226)
-- V23 cubria 7 de los 13 flags de forma_pago. Ahora f_cobrar_documento valida y
-- persiste TODOS los datos de dato_cobro parametrizables: cobrador, fecha/numero/
-- estado de deposito, motivo de rechazo y nota de credito (ntcr_documento).
-- La nota de credito ademas se valida como negocio: debe ser un documento NTCR
-- no anulado del MISMO cliente del cobro.
-- Parametros con DEFAULT NULL: f_cobrar_total y llamadas previas siguen validas.
-- ============================================================================

DROP FUNCTION IF EXISTS f_cobrar_documento(bigint,bigint,bigint,bigint,numeric,bigint,date,varchar,
  varchar,varchar,varchar,varchar,varchar,date,varchar);

CREATE OR REPLACE FUNCTION f_cobrar_documento(
  p_documento bigint, p_planilla bigint, p_forma_pago bigint, p_persona bigint,
  p_monto numeric, p_moneda bigint, p_fecha date, p_usuario varchar,
  p_emisor varchar DEFAULT NULL, p_procesador varchar DEFAULT NULL,
  p_numero varchar DEFAULT NULL, p_serie varchar DEFAULT NULL,
  p_cuenta varchar DEFAULT NULL, p_vencimiento date DEFAULT NULL,
  p_referencia varchar DEFAULT NULL,
  p_cobrador bigint DEFAULT NULL, p_fecha_deposito date DEFAULT NULL,
  p_numero_deposito varchar DEFAULT NULL, p_estado_deposito varchar DEFAULT NULL,
  p_motivo_rechazo varchar DEFAULT NULL, p_ntcr bigint DEFAULT NULL)
RETURNS bigint AS $$
DECLARE
  v_saldo numeric; v_empresa bigint; v_sucursal bigint; v_cobro bigint;
  v_pla_empresa bigint; v_pla_sucursal bigint; v_pla_estado varchar;
  v_fp forma_pago%ROWTYPE; v_ntcr_tipo varchar; v_ntcr_persona bigint; v_ntcr_estado varchar;
  v_hay_datos boolean;
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

  -- obs 225/226: datos exigibles parametrizados por la forma de pago (13 flags, doc 10).
  -- p_forma_pago NULL = efectivo sin especificar (sin flags que exigir).
  IF p_forma_pago IS NOT NULL THEN
    SELECT * INTO v_fp FROM forma_pago WHERE forma_pago = p_forma_pago;
    IF v_fp.forma_pago IS NULL THEN RAISE EXCEPTION 'Forma de pago inexistente'; END IF;
    IF v_fp.requiere_emisor          AND COALESCE(p_emisor, '')          = '' THEN RAISE EXCEPTION '% exige indicar el emisor',            v_fp.descripcion; END IF;
    IF v_fp.requiere_procesador      AND COALESCE(p_procesador, '')      = '' THEN RAISE EXCEPTION '% exige indicar el procesador',        v_fp.descripcion; END IF;
    IF v_fp.requiere_numero          AND COALESCE(p_numero, '')          = '' THEN RAISE EXCEPTION '% exige indicar el número',            v_fp.descripcion; END IF;
    IF v_fp.requiere_serie           AND COALESCE(p_serie, '')           = '' THEN RAISE EXCEPTION '% exige indicar la serie',             v_fp.descripcion; END IF;
    IF v_fp.requiere_vencimiento     AND p_vencimiento IS NULL               THEN RAISE EXCEPTION '% exige la fecha de vencimiento',       v_fp.descripcion; END IF;
    IF v_fp.requiere_cuenta          AND COALESCE(p_cuenta, '')          = '' THEN RAISE EXCEPTION '% exige indicar la cuenta',            v_fp.descripcion; END IF;
    IF v_fp.requiere_referencia      AND COALESCE(p_referencia, '')      = '' THEN RAISE EXCEPTION '% exige indicar la referencia',        v_fp.descripcion; END IF;
    IF v_fp.requiere_cobrador        AND p_cobrador IS NULL                  THEN RAISE EXCEPTION '% exige indicar el cobrador',           v_fp.descripcion; END IF;
    IF v_fp.requiere_fecha_deposito  AND p_fecha_deposito IS NULL            THEN RAISE EXCEPTION '% exige la fecha de depósito',          v_fp.descripcion; END IF;
    IF v_fp.requiere_numero_deposito AND COALESCE(p_numero_deposito, '') = '' THEN RAISE EXCEPTION '% exige el número de depósito',        v_fp.descripcion; END IF;
    IF v_fp.requiere_estado_deposito AND COALESCE(p_estado_deposito, '') = '' THEN RAISE EXCEPTION '% exige el estado del depósito',       v_fp.descripcion; END IF;
    IF v_fp.requiere_motivo_rechazo  AND COALESCE(p_motivo_rechazo, '')  = '' THEN RAISE EXCEPTION '% exige el motivo de rechazo',         v_fp.descripcion; END IF;
    IF v_fp.requiere_nota_credito    AND p_ntcr IS NULL                      THEN RAISE EXCEPTION '% exige la nota de crédito asociada',   v_fp.descripcion; END IF;
  END IF;

  -- Nota de credito: debe ser un documento NTCR no anulado del MISMO cliente
  IF p_ntcr IS NOT NULL THEN
    SELECT tipo_codigo, persona, estado INTO v_ntcr_tipo, v_ntcr_persona, v_ntcr_estado
      FROM documento WHERE documento = p_ntcr;
    IF v_ntcr_tipo IS NULL THEN RAISE EXCEPTION 'La nota de crédito no existe'; END IF;
    IF v_ntcr_tipo <> 'NTCR' THEN RAISE EXCEPTION 'El documento asociado no es una nota de crédito'; END IF;
    IF v_ntcr_estado = 'ANULADO' THEN RAISE EXCEPTION 'La nota de crédito está anulada'; END IF;
    IF p_persona IS NOT NULL AND v_ntcr_persona <> p_persona THEN
      RAISE EXCEPTION 'La nota de crédito pertenece a otro cliente';
    END IF;
  END IF;

  INSERT INTO cobro (empresa, sucursal, planilla, forma_pago, persona, cajero, fecha, hora, monto,
                     moneda, usuario_creacion, fecha_creacion)
    VALUES (v_empresa, v_sucursal, p_planilla, p_forma_pago, p_persona, p_usuario, p_fecha, now(),
            p_monto, p_moneda, p_usuario, now())
    RETURNING cobro INTO v_cobro;

  INSERT INTO cobro_detalle (cobro, secuencia, documento, monto, usuario_creacion, fecha_creacion)
    VALUES (v_cobro, 1, p_documento, p_monto, p_usuario, now());

  -- dato_cobro junto al cobro cuando el medio de pago trae datos (obs 225/226)
  v_hay_datos := COALESCE(p_emisor,'') <> '' OR COALESCE(p_procesador,'') <> ''
      OR COALESCE(p_numero,'') <> '' OR COALESCE(p_serie,'') <> '' OR COALESCE(p_cuenta,'') <> ''
      OR p_vencimiento IS NOT NULL OR COALESCE(p_referencia,'') <> ''
      OR p_cobrador IS NOT NULL OR p_fecha_deposito IS NOT NULL
      OR COALESCE(p_numero_deposito,'') <> '' OR COALESCE(p_estado_deposito,'') <> ''
      OR COALESCE(p_motivo_rechazo,'') <> '' OR p_ntcr IS NOT NULL;
  IF v_hay_datos THEN
    INSERT INTO dato_cobro (cobro, emisor_codigo, procesador_codigo, numero, serie,
                            cuenta_corriente, fecha_vencimiento, referencia,
                            cobrador, fecha_deposito, numero_deposito, estado_deposito,
                            motivo_rechazo_codigo, ntcr_documento,
                            usuario_creacion, fecha_creacion)
      VALUES (v_cobro, NULLIF(p_emisor,''), NULLIF(p_procesador,''), NULLIF(p_numero,''),
              NULLIF(p_serie,''), NULLIF(p_cuenta,''), p_vencimiento, NULLIF(p_referencia,''),
              p_cobrador, p_fecha_deposito, NULLIF(p_numero_deposito,''), NULLIF(p_estado_deposito,''),
              NULLIF(p_motivo_rechazo,''), p_ntcr,
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
