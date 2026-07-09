-- ============================================================================
-- V27 — SPs del motor adaptados al esquema multiempresa V26 (F3, REQ-0035)
-- Solo 3 funciones referencian columnas renombradas por V26:
--   * f_siguiente_numero: rango_comprobante empresa->tenant, tipo_codigo->tipo.
--   * f_cobrar_documento: planilla/cobro/documento empresa->tenant (coherencia de
--     tenant reforzada: la planilla y el documento deben ser del MISMO tenant),
--     documento.tipo_codigo->tipo, y dato_cobro emisor/procesador/motivo_rechazo
--     por id (resueltos desde el codigo por (lista,codigo,tenant IN(-1,tenant))).
--   * f_anular_cobro: cobro empresa->tenant; anulacion empresa->tenant y motivo por id.
-- Las demas funciones (f_cuadrar_documento, f_actualiza_saldo_cuotas, f_mora_cuota,
-- f_generar_cronograma, f_cobrar_total, triggers) NO tocan columnas cambiadas.
-- Va como unidad desplegable con V26 (staging en tools/multiempresa/).
-- ============================================================================

-- ── f_siguiente_numero: numera por tenant + tipo directo ────────────────────
-- (el parametro conserva el nombre historico p_empresa por CREATE OR REPLACE, pero
--  ahora es el TENANT; se filtra por la columna rango_comprobante.tenant).
CREATE OR REPLACE FUNCTION public.f_siguiente_numero(p_empresa bigint, p_tipo character varying, p_serie character varying)
 RETURNS bigint
 LANGUAGE plpgsql
AS $function$
DECLARE
  v_rango bigint;
  v_numero bigint;
  v_hasta bigint;
BEGIN
  SELECT rango_comprobante, numero_actual, numero_hasta INTO v_rango, v_numero, v_hasta
    FROM rango_comprobante
    WHERE tenant = p_empresa AND tipo = p_tipo AND serie = p_serie AND estado = 'ACTIVO'
    ORDER BY numero_desde
    FOR UPDATE
    LIMIT 1;
  IF v_rango IS NULL THEN
    RAISE EXCEPTION 'No hay timbrado ACTIVO para el tipo % serie %', p_tipo, p_serie;
  END IF;
  IF v_numero > v_hasta THEN
    RAISE EXCEPTION 'El timbrado del tipo % serie % agoto su rango (llego a %)', p_tipo, p_serie, v_hasta;
  END IF;
  UPDATE rango_comprobante SET numero_actual = v_numero + 1, version = version + 1
    WHERE rango_comprobante = v_rango;
  RETURN v_numero;
END;
$function$;

-- ── f_cobrar_documento: tenant en planilla/cobro/documento + refs por id ─────
CREATE OR REPLACE FUNCTION public.f_cobrar_documento(p_documento bigint, p_planilla bigint, p_forma_pago bigint, p_persona bigint, p_monto numeric, p_moneda bigint, p_fecha date, p_usuario character varying, p_emisor character varying DEFAULT NULL::character varying, p_procesador character varying DEFAULT NULL::character varying, p_numero character varying DEFAULT NULL::character varying, p_serie character varying DEFAULT NULL::character varying, p_cuenta character varying DEFAULT NULL::character varying, p_vencimiento date DEFAULT NULL::date, p_referencia character varying DEFAULT NULL::character varying, p_cobrador bigint DEFAULT NULL::bigint, p_fecha_deposito date DEFAULT NULL::date, p_numero_deposito character varying DEFAULT NULL::character varying, p_estado_deposito character varying DEFAULT NULL::character varying, p_motivo_rechazo character varying DEFAULT NULL::character varying, p_ntcr bigint DEFAULT NULL::bigint)
 RETURNS bigint
 LANGUAGE plpgsql
AS $function$
DECLARE
  v_saldo numeric; v_tenant bigint; v_sucursal bigint; v_cobro bigint;
  v_pla_tenant bigint; v_pla_sucursal bigint; v_pla_estado varchar;
  v_fp forma_pago%ROWTYPE; v_ntcr_tipo varchar; v_ntcr_persona bigint; v_ntcr_estado varchar;
  v_ntcr_tenant bigint;
  v_hay_datos boolean;
  v_emisor bigint; v_procesador bigint; v_motivo_rechazo bigint;
BEGIN
  -- La planilla debe existir (FOR UPDATE), estar ABIERTA y coincidir en tenant/sucursal.
  SELECT tenant, sucursal, estado INTO v_pla_tenant, v_pla_sucursal, v_pla_estado
    FROM planilla WHERE planilla = p_planilla FOR UPDATE;
  IF v_pla_estado IS NULL THEN RAISE EXCEPTION 'La planilla de caja no existe'; END IF;
  IF v_pla_estado <> 'ABIERTA' THEN RAISE EXCEPTION 'La planilla de caja no está abierta'; END IF;

  SELECT saldo, tenant, sucursal INTO v_saldo, v_tenant, v_sucursal
    FROM documento WHERE documento = p_documento AND estado <> 'ANULADO' FOR UPDATE;
  IF v_saldo IS NULL THEN RAISE EXCEPTION 'Documento inexistente o anulado'; END IF;
  IF v_saldo <= 0 THEN RAISE EXCEPTION 'El saldo del comprobante es 0 o menor a cero'; END IF;
  IF p_monto <= 0 THEN RAISE EXCEPTION 'El monto debe ser mayor a cero'; END IF;
  IF v_saldo < p_monto THEN RAISE EXCEPTION 'El monto que desea pagar es mayor al saldo de esta factura'; END IF;
  -- Coherencia de tenant (multiempresa): la planilla y el documento son del mismo tenant.
  IF v_tenant <> v_pla_tenant OR v_sucursal <> v_pla_sucursal THEN
    RAISE EXCEPTION 'La planilla de caja pertenece a otra empresa/sucursal';
  END IF;

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

  IF p_ntcr IS NOT NULL THEN
    SELECT tipo, persona, estado, tenant INTO v_ntcr_tipo, v_ntcr_persona, v_ntcr_estado, v_ntcr_tenant
      FROM documento WHERE documento = p_ntcr;
    IF v_ntcr_tipo IS NULL THEN RAISE EXCEPTION 'La nota de crédito no existe'; END IF;
    IF v_ntcr_tipo <> 'NTCR' THEN RAISE EXCEPTION 'El documento asociado no es una nota de crédito'; END IF;
    IF v_ntcr_estado = 'ANULADO' THEN RAISE EXCEPTION 'La nota de crédito está anulada'; END IF;
    -- Coherencia de tenant (obs 247): la NTCR debe ser de la MISMA empresa que el documento
    -- cobrado; si no, quedaria una referencia cruzada entre empresas en dato_cobro.ntcr_documento.
    IF v_ntcr_tenant <> v_tenant THEN
      RAISE EXCEPTION 'La nota de crédito pertenece a otra empresa';
    END IF;
    IF p_persona IS NOT NULL AND v_ntcr_persona <> p_persona THEN
      RAISE EXCEPTION 'La nota de crédito pertenece a otro cliente';
    END IF;
  END IF;

  INSERT INTO cobro (tenant, sucursal, planilla, forma_pago, persona, cajero, fecha, hora, monto,
                     moneda, usuario_creacion, fecha_creacion)
    VALUES (v_tenant, v_sucursal, p_planilla, p_forma_pago, p_persona, p_usuario, p_fecha, now(),
            p_monto, p_moneda, p_usuario, now())
    RETURNING cobro INTO v_cobro;

  INSERT INTO cobro_detalle (cobro, secuencia, documento, monto, usuario_creacion, fecha_creacion)
    VALUES (v_cobro, 1, p_documento, p_monto, p_usuario, now());

  v_hay_datos := COALESCE(p_emisor,'') <> '' OR COALESCE(p_procesador,'') <> ''
      OR COALESCE(p_numero,'') <> '' OR COALESCE(p_serie,'') <> '' OR COALESCE(p_cuenta,'') <> ''
      OR p_vencimiento IS NOT NULL OR COALESCE(p_referencia,'') <> ''
      OR p_cobrador IS NOT NULL OR p_fecha_deposito IS NOT NULL
      OR COALESCE(p_numero_deposito,'') <> '' OR COALESCE(p_estado_deposito,'') <> ''
      OR COALESCE(p_motivo_rechazo,'') <> '' OR p_ntcr IS NOT NULL;
  IF v_hay_datos THEN
    -- V26: emisor/procesador/motivo_rechazo son id de entidad. Se resuelven desde el codigo
    -- por (lista, codigo, tenant IN(-1, tenant del documento)) PREFIRIENDO la opcion propia
    -- del tenant sobre la global. Si el codigo viene NO vacio y no existe para el tenant
    -- visible, se ABORTA (obs 246): antes la FK compuesta contra entidad lo rechazaba; ahora
    -- resolver a NULL en silencio perderia la trazabilidad del medio de pago.
    IF COALESCE(p_emisor,'') <> '' THEN
      SELECT e.entidad INTO v_emisor FROM entidad e
        WHERE e.lista='EMISORES' AND e.codigo=p_emisor AND e.tenant IN (-1, v_tenant)
        ORDER BY (e.tenant = v_tenant) DESC LIMIT 1;
      IF v_emisor IS NULL THEN RAISE EXCEPTION 'El emisor % no existe en EMISORES para la empresa', p_emisor; END IF;
    END IF;
    IF COALESCE(p_procesador,'') <> '' THEN
      SELECT e.entidad INTO v_procesador FROM entidad e
        WHERE e.lista='PROCESADORES' AND e.codigo=p_procesador AND e.tenant IN (-1, v_tenant)
        ORDER BY (e.tenant = v_tenant) DESC LIMIT 1;
      IF v_procesador IS NULL THEN RAISE EXCEPTION 'El procesador % no existe en PROCESADORES para la empresa', p_procesador; END IF;
    END IF;
    IF COALESCE(p_motivo_rechazo,'') <> '' THEN
      SELECT e.entidad INTO v_motivo_rechazo FROM entidad e
        WHERE e.lista='MOTIVOS_RECHAZO' AND e.codigo=p_motivo_rechazo AND e.tenant IN (-1, v_tenant)
        ORDER BY (e.tenant = v_tenant) DESC LIMIT 1;
      IF v_motivo_rechazo IS NULL THEN RAISE EXCEPTION 'El motivo de rechazo % no existe en MOTIVOS_RECHAZO para la empresa', p_motivo_rechazo; END IF;
    END IF;
    INSERT INTO dato_cobro (cobro, emisor, procesador, numero, serie,
                            cuenta_corriente, fecha_vencimiento, referencia,
                            cobrador, fecha_deposito, numero_deposito, estado_deposito,
                            motivo_rechazo, ntcr_documento,
                            usuario_creacion, fecha_creacion)
      VALUES (v_cobro, v_emisor, v_procesador,
              NULLIF(p_numero,''), NULLIF(p_serie,''), NULLIF(p_cuenta,''), p_vencimiento, NULLIF(p_referencia,''),
              p_cobrador, p_fecha_deposito, NULLIF(p_numero_deposito,''), NULLIF(p_estado_deposito,''),
              v_motivo_rechazo, p_ntcr, p_usuario, now());
  END IF;

  UPDATE documento SET saldo = saldo - p_monto, version = version + 1 WHERE documento = p_documento;
  UPDATE planilla SET monto_cobro = monto_cobro + p_monto, version = version + 1 WHERE planilla = p_planilla;
  PERFORM f_actualiza_saldo_cuotas(p_documento);
  RETURN v_cobro;
END;
$function$;

-- ── f_anular_cobro: cobro por tenant; anulacion por tenant + motivo por id ───
CREATE OR REPLACE FUNCTION public.f_anular_cobro(p_cobro bigint, p_usuario character varying, p_motivo character varying DEFAULT NULL::character varying)
 RETURNS void
 LANGUAGE plpgsql
AS $function$
DECLARE
  r_det record; v_tenant bigint; v_motivo bigint;
BEGIN
  IF COALESCE(p_motivo, '') = '' THEN
    RAISE EXCEPTION 'El motivo de la anulación es obligatorio';
  END IF;

  SELECT tenant INTO v_tenant FROM cobro WHERE cobro = p_cobro AND estado = 'ACTIVO' FOR UPDATE;
  IF v_tenant IS NULL THEN
    RAISE EXCEPTION 'El cobro no existe o ya esta anulado';
  END IF;

  -- motivo por id: debe existir en MOTIVOS_ANULACION visible al tenant (-1 o propio).
  SELECT e.entidad INTO v_motivo FROM entidad e
    WHERE e.lista = 'MOTIVOS_ANULACION' AND e.codigo = p_motivo AND e.tenant IN (-1, v_tenant) LIMIT 1;
  IF v_motivo IS NULL THEN
    RAISE EXCEPTION 'El motivo de anulación % no existe en MOTIVOS_ANULACION', p_motivo;
  END IF;

  FOR r_det IN SELECT documento, monto FROM cobro_detalle WHERE cobro = p_cobro AND estado = 'ACTIVO' LOOP
    UPDATE documento SET saldo = LEAST(saldo + r_det.monto, total), version = version + 1
      WHERE documento = r_det.documento;
    PERFORM f_actualiza_saldo_cuotas(r_det.documento);
  END LOOP;

  UPDATE planilla p SET monto_cobro = p.monto_cobro - c.monto, version = p.version + 1
    FROM cobro c WHERE c.cobro = p_cobro AND p.planilla = c.planilla;

  UPDATE cobro_detalle SET estado = 'ANULADO', usuario_modificacion = p_usuario, fecha_modificacion = now()
    WHERE cobro = p_cobro;
  UPDATE cobro SET estado = 'ANULADO', usuario_modificacion = p_usuario, fecha_modificacion = now()
    WHERE cobro = p_cobro;

  INSERT INTO anulacion (tenant, cobro, motivo, fecha, usuario_creacion, fecha_creacion)
    VALUES (v_tenant, p_cobro, v_motivo, now(), p_usuario, now());
END;
$function$;
