-- V16 (REQ-0016/0017): motor de documentos EN LA BASE (arquitectura BD-centrica, doc 10).
-- Toda la logica de cuadre vive como triggers; la generacion y numeracion como funciones.
-- Java solo INVOCA estas funciones, nunca reimplementa el cuadre.

-- ── Trigger de cuadre del documento: total y saldo salen de los detalles ──
-- El saldo baja con los cobros (SP de V17); el detalle define el TOTAL. Al insertar/editar
-- detalles se recalcula el total y, si el documento aun no tuvo cobros, el saldo = total.
CREATE OR REPLACE FUNCTION f_cuadrar_documento(p_documento bigint) RETURNS void AS $$
DECLARE
  v_total numeric(15,2);
  v_cobrado numeric(15,2);
BEGIN
  SELECT COALESCE(SUM(monto),0) INTO v_total FROM documento_detalle WHERE documento = p_documento;
  -- lo ya cobrado = total - saldo actual (preserva cobros parciales)
  SELECT total - saldo INTO v_cobrado FROM documento WHERE documento = p_documento;
  IF v_cobrado IS NULL THEN v_cobrado := 0; END IF;
  UPDATE documento
    SET total = v_total,
        saldo = GREATEST(v_total - v_cobrado, 0),
        estado = CASE WHEN estado = 'ANULADO' THEN 'ANULADO'
                      WHEN GREATEST(v_total - v_cobrado, 0) <= 0 AND v_total > 0 THEN 'CANCELADO'
                      ELSE 'PENDIENTE' END,
        version = version + 1
    WHERE documento = p_documento;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE FUNCTION tg_documento_detalle_cuadre() RETURNS trigger AS $$
BEGIN
  IF TG_OP = 'DELETE' THEN
    PERFORM f_cuadrar_documento(OLD.documento);
    RETURN OLD;
  END IF;
  PERFORM f_cuadrar_documento(NEW.documento);
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER td_documento_detalle_cuadre
  AFTER INSERT OR UPDATE OR DELETE ON documento_detalle
  FOR EACH ROW EXECUTE FUNCTION tg_documento_detalle_cuadre();

-- ── Trigger: el estado del documento SIEMPRE se deriva del saldo (nunca a mano) ──
CREATE OR REPLACE FUNCTION tg_documento_estado() RETURNS trigger AS $$
BEGIN
  IF NEW.estado <> 'ANULADO' THEN
    NEW.estado := CASE WHEN NEW.saldo <= 0 AND NEW.total > 0 THEN 'CANCELADO' ELSE 'PENDIENTE' END;
  END IF;
  RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER td_documento_estado
  BEFORE UPDATE OF saldo ON documento
  FOR EACH ROW EXECUTE FUNCTION tg_documento_estado();

-- ── Numeracion por timbrado (rango_comprobante) con bloqueo FOR UPDATE ──
CREATE OR REPLACE FUNCTION f_siguiente_numero(p_empresa bigint, p_tipo varchar, p_serie varchar)
RETURNS bigint AS $$
DECLARE
  v_rango bigint;
  v_numero bigint;
  v_hasta bigint;
BEGIN
  SELECT rango_comprobante, numero_actual, numero_hasta INTO v_rango, v_numero, v_hasta
    FROM rango_comprobante
    WHERE empresa = p_empresa AND tipo_codigo = p_tipo AND serie = p_serie AND estado = 'ACTIVO'
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
$$ LANGUAGE plpgsql;

-- ── Generacion de cronograma de cuotas (REQ-0017) ──
-- Reparte el saldo financiado en N cuotas mensuales desde una fecha, con dia_pago fijo.
-- La ultima cuota absorbe el redondeo para que la suma cuadre EXACTA al total.
CREATE OR REPLACE FUNCTION f_generar_cronograma(
  p_operacion bigint, p_cantidad int, p_monto_total numeric, p_fecha_primera date,
  p_dia_pago int, p_moneda bigint, p_usuario varchar)
RETURNS int AS $$
DECLARE
  v_cuota numeric(15,2);
  v_acumulado numeric(15,2) := 0;
  v_monto numeric(15,2);
  v_fecha date;
  i int;
BEGIN
  IF p_cantidad < 1 THEN RAISE EXCEPTION 'La cantidad de cuotas debe ser mayor a cero'; END IF;
  -- limpia cronograma previo si no tuvo cobros
  IF EXISTS (SELECT 1 FROM cronograma_cuota WHERE operacion = p_operacion AND saldo <> monto) THEN
    RAISE EXCEPTION 'La operacion ya tiene cuotas con cobros; use regeneracion (REQ-0019)';
  END IF;
  DELETE FROM cronograma_cuota WHERE operacion = p_operacion;
  v_cuota := round(p_monto_total / p_cantidad, 2);
  FOR i IN 1..p_cantidad LOOP
    v_fecha := (p_fecha_primera + ((i-1) || ' month')::interval)::date;
    IF p_dia_pago IS NOT NULL AND p_dia_pago BETWEEN 1 AND 28 THEN
      v_fecha := date_trunc('month', v_fecha)::date + (p_dia_pago - 1);
    END IF;
    IF i = p_cantidad THEN
      v_monto := p_monto_total - v_acumulado;   -- ultima cuota cuadra el total exacto
    ELSE
      v_monto := v_cuota;
    END IF;
    v_acumulado := v_acumulado + v_monto;
    INSERT INTO cronograma_cuota (operacion, numero_cuota, fecha_vencimiento, monto, saldo,
                                  moneda, usuario_creacion, fecha_creacion)
      VALUES (p_operacion, i, v_fecha, v_monto, v_monto, p_moneda, p_usuario, now());
  END LOOP;
  RETURN p_cantidad;
END;
$$ LANGUAGE plpgsql;

-- ── Vista de saldos por operacion (para reportes/estado de cuenta) ──
CREATE OR REPLACE VIEW v_operacion_saldo AS
SELECT o.operacion,
       o.cliente,
       o.monto_total_operacion,
       COALESCE(SUM(c.monto),0)  AS total_cuotas,
       COALESCE(SUM(c.saldo),0)  AS saldo_pendiente,
       COALESCE(SUM(CASE WHEN c.estado='CANCELADO' THEN c.monto ELSE 0 END),0) AS total_cancelado,
       COUNT(c.cronograma_cuota) FILTER (WHERE c.estado='PENDIENTE') AS cuotas_pendientes
FROM operacion o
LEFT JOIN cronograma_cuota c ON c.operacion = o.operacion
GROUP BY o.operacion, o.cliente, o.monto_total_operacion;
