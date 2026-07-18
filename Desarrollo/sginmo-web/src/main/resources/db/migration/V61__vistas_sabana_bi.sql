-- REQ-0105 - Capa analitica: vistas "sabana" para BI (Metabase / Excel / cualquier consumidor).
--
-- Principio de diseno: una sabana = UN grano, TODAS las filas, MUCHAS columnas derivadas.
-- No se pre-filtra nada (nada de "solo vencidas" o "solo libres"): el cubo filtra. Asi una sola
-- vista reemplaza los ~30 SQL escritos a mano que hoy consultan las mismas 4 tablas con distinto
-- filtro, y desaparecen las incoherencias entre un KPI y su drill-down.
--
-- SEGURIDAD: son vistas SECURITY INVOKER (default), asi que la RLS (V28) sigue aplicando segun el
-- rol que consulta. El consumidor BI debe conectarse fijando el tenant, p.ej. en la URL JDBC:
--   jdbc:postgresql://host:5432/sginmo?options=-c%20app.tenant%3D1
-- Sin app.tenant la RLS es fail-closed y las vistas devuelven 0 filas (comportamiento deseado).

-- ─────────────────────────────────────────────────────────────────────────────
-- 1) v_sabana_cuota - grano CUOTA. Cubre mora, aging, estado de cuenta,
--    cobranza esperada y flujo proyectado (incluye cuotas futuras, que hoy
--    ningun reporte mira).
-- ─────────────────────────────────────────────────────────────────────────────
CREATE OR REPLACE VIEW v_sabana_cuota AS
SELECT
    cc.cronograma_cuota,
    cc.numero_cuota,
    cc.fecha_vencimiento,
    cc.monto,
    cc.saldo,
    (cc.monto - cc.saldo)                                   AS monto_cobrado,
    cc.estado                                               AS estado_cuota,
    cc.fecha_cancelacion,
    cc.documento,
    cc.moneda,
    mo.descripcion                                          AS moneda_desc,
    -- dimensiones de tiempo (para tablas dinamicas)
    EXTRACT(YEAR  FROM cc.fecha_vencimiento)::int           AS anio_vencimiento,
    EXTRACT(MONTH FROM cc.fecha_vencimiento)::int           AS mes_vencimiento,
    to_char(cc.fecha_vencimiento, 'YYYY-MM')                AS periodo_vencimiento,
    to_char(cc.fecha_cancelacion, 'YYYY-MM')                AS periodo_cobro,
    -- estado de cobranza derivado
    (cc.estado = 'PENDIENTE' AND cc.saldo > 0 AND cc.fecha_vencimiento <  current_date) AS esta_vencida,
    (cc.estado = 'PENDIENTE' AND cc.saldo > 0 AND cc.fecha_vencimiento >= current_date) AS es_futura,
    CASE WHEN cc.estado = 'PENDIENTE' AND cc.saldo > 0 AND cc.fecha_vencimiento < current_date
         THEN (current_date - cc.fecha_vencimiento) ELSE 0 END                          AS dias_mora,
    CASE WHEN cc.estado = 'CANCELADO' OR cc.saldo <= 0            THEN 'COBRADA'
         WHEN cc.fecha_vencimiento >= current_date               THEN 'POR VENCER'
         WHEN current_date - cc.fecha_vencimiento <= 30          THEN '01-30'
         WHEN current_date - cc.fecha_vencimiento <= 60          THEN '31-60'
         WHEN current_date - cc.fecha_vencimiento <= 90          THEN '61-90'
         ELSE '90+' END                                                                 AS tramo_aging,
    -- operacion / contrato
    o.operacion, o.tipo_operacion, o.condicion_operacion,
    o.estado                                                AS estado_operacion,
    o.fecha_operacion, o.fecha_inicio_contrato, o.fecha_fin_contrato, o.plazo, o.dia_pago,
    o.tenant, o.sucursal,
    su.descripcion                                          AS sucursal_desc,
    -- cliente
    o.cliente, pc.nombre AS cliente_nombre, pc.numero_documento AS cliente_documento,
    -- activo
    a.activo, a.nombre AS activo_nombre,
    a.tipo AS activo_tipo, ta.descripcion AS activo_tipo_desc,
    a.direccion AS activo_direccion,
    a.padre AS activo_padre, ap.nombre AS activo_padre_nombre,
    a.ubicacion, ug.nombre AS zona,
    -- propietario (el primero activo; una propiedad puede tener varios)
    pr.propietario, pp.nombre AS propietario_nombre
FROM cronograma_cuota cc
JOIN      operacion o  ON o.operacion = cc.operacion
LEFT JOIN moneda    mo ON mo.moneda   = cc.moneda
LEFT JOIN sucursal  su ON su.sucursal = o.sucursal
LEFT JOIN v_persona pc ON pc.persona  = o.cliente
LEFT JOIN activo    a  ON a.activo    = o.activo
LEFT JOIN entidad   ta ON ta.entidad  = a.tipo
LEFT JOIN activo    ap ON ap.activo   = a.padre
LEFT JOIN ubicacion_geografica ug ON ug.ubicacion_geografica = a.ubicacion
LEFT JOIN LATERAL (SELECT x.propietario FROM activo_propietario x
                    WHERE x.activo = a.activo AND x.estado = 'ACTIVO'
                    ORDER BY x.activo_propietario LIMIT 1) pr ON true
LEFT JOIN v_persona pp ON pp.persona = pr.propietario;

COMMENT ON VIEW v_sabana_cuota IS 'REQ-0105 Sabana grano cuota: mora, aging, cobranza esperada y flujo proyectado.';

-- ─────────────────────────────────────────────────────────────────────────────
-- 2) v_sabana_cobro - grano COBRO. Recaudacion: reemplaza los 10 SELECT sobre
--    cobro que hoy difieren solo en el filtro.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE OR REPLACE VIEW v_sabana_cobro AS
SELECT
    c.cobro, c.fecha, c.hora, c.monto, c.estado AS estado_cobro,
    c.concepto, c.cajero, c.recibo_documento, c.factura_documento,
    c.tenant, c.sucursal, su.descripcion AS sucursal_desc,
    c.planilla, pl.estado AS planilla_estado, pl.fecha_apertura AS planilla_fecha,
    c.moneda, mo.descripcion AS moneda_desc,
    c.forma_pago, fp.descripcion AS forma_pago_desc,
    -- tiempo
    EXTRACT(YEAR  FROM c.fecha)::int  AS anio,
    EXTRACT(MONTH FROM c.fecha)::int  AS mes,
    to_char(c.fecha, 'YYYY-MM')       AS periodo,
    to_char(c.fecha, 'TMDay')         AS dia_semana,
    -- persona que paga
    c.persona, pc.nombre AS cliente_nombre, pc.numero_documento AS cliente_documento,
    -- contrato/activo alcanzado (via el documento de cuenta corriente que se cobro)
    op.operacion, op.tipo_operacion, op.activo, ac.nombre AS activo_nombre,
    ta.descripcion AS activo_tipo_desc, ac.ubicacion, ug.nombre AS zona
FROM cobro c
LEFT JOIN sucursal   su ON su.sucursal   = c.sucursal
LEFT JOIN planilla   pl ON pl.planilla   = c.planilla
LEFT JOIN moneda     mo ON mo.moneda     = c.moneda
LEFT JOIN forma_pago fp ON fp.forma_pago = c.forma_pago
LEFT JOIN v_persona  pc ON pc.persona    = c.persona
-- un cobro aplica a documentos; el DINT/OP de cuenta corriente es 1 por operacion
LEFT JOIN LATERAL (
    SELECT DISTINCT cq.operacion
      FROM cobro_detalle cd
      JOIN cronograma_cuota cq ON cq.documento = cd.documento
     WHERE cd.cobro = c.cobro
     LIMIT 1) d ON true
LEFT JOIN operacion op ON op.operacion = d.operacion
LEFT JOIN activo    ac ON ac.activo    = op.activo
LEFT JOIN entidad   ta ON ta.entidad   = ac.tipo
LEFT JOIN ubicacion_geografica ug ON ug.ubicacion_geografica = ac.ubicacion;

COMMENT ON VIEW v_sabana_cobro IS 'REQ-0105 Sabana grano cobro: recaudacion por periodo/forma/sucursal/activo.';

-- ─────────────────────────────────────────────────────────────────────────────
-- 3) v_sabana_activo - grano ACTIVO. Cartera de inmuebles, ocupacion y vacancia
--    con UNA sola definicion de "disponible" (hoy hay dos incompatibles).
-- ─────────────────────────────────────────────────────────────────────────────
CREATE OR REPLACE VIEW v_sabana_activo AS
SELECT
    a.activo, a.nombre AS activo_nombre, a.tenant,
    a.tipo, ta.descripcion AS activo_tipo_desc,
    a.padre, ap.nombre AS activo_padre_nombre,
    a.direccion, a.ubicacion, ug.nombre AS zona,
    a.estado AS estado_activo,
    a.precio_venta, a.precio_alquiler, a.comision_venta, a.comision_alquiler,
    a.cuenta_catastral, a.numero_finca, a.numero_lote, a.numero_manzana,
    a.superficie, a.m2_construccion, a.anio, a.cantidad_unidades,
    -- definicion UNICA de disponibilidad comercial
    (a.precio_alquiler > 0 AND a.estado <> 'VENDIDA')       AS es_alquilable,
    (ov.operacion IS NOT NULL)                              AS esta_ocupado,
    CASE WHEN a.estado = 'VENDIDA'                          THEN 'VENDIDA'
         WHEN ov.operacion IS NOT NULL                      THEN 'OCUPADA'
         WHEN a.precio_alquiler > 0                         THEN 'VACANTE ALQUILABLE'
         ELSE 'LIBRE NO ALQUILABLE' END                     AS situacion_comercial,
    -- contrato vigente (si lo hay)
    ov.operacion AS operacion_vigente, ov.tipo_operacion AS operacion_tipo,
    ov.fecha_inicio_contrato, ov.fecha_fin_contrato,
    ov.cliente AS inquilino, pi.nombre AS inquilino_nombre,
    CASE WHEN ov.fecha_fin_contrato IS NOT NULL
         THEN (ov.fecha_fin_contrato - current_date) END    AS dias_para_vencer,
    -- propietario
    pr.propietario, pp.nombre AS propietario_nombre
FROM activo a
LEFT JOIN entidad ta ON ta.entidad = a.tipo
LEFT JOIN activo  ap ON ap.activo  = a.padre
LEFT JOIN ubicacion_geografica ug ON ug.ubicacion_geografica = a.ubicacion
LEFT JOIN LATERAL (SELECT o.* FROM operacion o
                    WHERE o.activo = a.activo AND o.estado = 'VIGENTE'
                    ORDER BY o.fecha_operacion DESC LIMIT 1) ov ON true
LEFT JOIN v_persona pi ON pi.persona = ov.cliente
LEFT JOIN LATERAL (SELECT x.propietario FROM activo_propietario x
                    WHERE x.activo = a.activo AND x.estado = 'ACTIVO'
                    ORDER BY x.activo_propietario LIMIT 1) pr ON true
LEFT JOIN v_persona pp ON pp.persona = pr.propietario;

COMMENT ON VIEW v_sabana_activo IS 'REQ-0105 Sabana grano activo: cartera, ocupacion, vacancia y contrato vigente.';

-- ─────────────────────────────────────────────────────────────────────────────
-- 4) v_sabana_movimiento - grano INGRESO/EGRESO. Rentabilidad y gastos, con el
--    signo neto y la marca de pasivo resueltos en un solo lugar.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE OR REPLACE VIEW v_sabana_movimiento AS
SELECT
    ie.ingreso_egreso, ie.fecha, ie.tipo AS tipo_movimiento, ie.estado AS estado_movimiento,
    ie.monto, ie.saldo, ie.observacion, ie.tenant, ie.documento, ie.tipo_imputacion,
    CASE WHEN ie.tipo = 'INGRESO' THEN ie.monto ELSE -ie.monto END AS monto_neto,
    -- tiempo
    EXTRACT(YEAR  FROM ie.fecha)::int AS anio,
    EXTRACT(MONTH FROM ie.fecha)::int AS mes,
    to_char(ie.fecha, 'YYYY-MM')      AS periodo,
    -- concepto
    ie.articulo, ar.descripcion AS articulo_desc, ar.aplicacion,
    (ar.aplicacion IN ('DEPOSITO_GARANTIA', 'GARANTIA'))          AS es_pasivo,
    -- contraparte / imputacion
    ie.persona, pe.nombre AS persona_nombre,
    ie.activo, ac.nombre AS activo_nombre, ta.descripcion AS activo_tipo_desc,
    ac.ubicacion, ug.nombre AS zona,
    ie.operacion, op.tipo_operacion,
    ie.forma_pago, fp.descripcion AS forma_pago_desc
FROM ingreso_egreso ie
LEFT JOIN articulo   ar ON ar.articulo   = ie.articulo
LEFT JOIN v_persona  pe ON pe.persona    = ie.persona
LEFT JOIN activo     ac ON ac.activo     = ie.activo
LEFT JOIN entidad    ta ON ta.entidad    = ac.tipo
LEFT JOIN ubicacion_geografica ug ON ug.ubicacion_geografica = ac.ubicacion
LEFT JOIN operacion  op ON op.operacion  = ie.operacion
LEFT JOIN forma_pago fp ON fp.forma_pago = ie.forma_pago;

COMMENT ON VIEW v_sabana_movimiento IS 'REQ-0105 Sabana grano movimiento: ingresos/egresos, rentabilidad por activo/aplicacion.';

-- ─────────────────────────────────────────────────────────────────────────────
-- 5) v_sabana_contrato - grano OPERACION, con el avance del cronograma
--    resuelto. Cubre contratos por vencer, renovaciones y cartera por contrato.
-- ─────────────────────────────────────────────────────────────────────────────
CREATE OR REPLACE VIEW v_sabana_contrato AS
SELECT
    o.operacion, o.tipo_operacion, o.condicion_operacion, o.estado AS estado_operacion,
    o.fecha_operacion, o.fecha_inicio_contrato, o.fecha_fin_contrato, o.fecha_finalizacion,
    o.fecha_renovacion, o.plazo, o.dia_pago, o.dias_gracia, o.monto_mora,
    o.precio, o.monto_total_operacion, o.garantia,
    o.tenant, o.sucursal, su.descripcion AS sucursal_desc,
    o.moneda, mo.descripcion AS moneda_desc,
    -- tiempo
    EXTRACT(YEAR FROM o.fecha_operacion)::int AS anio_operacion,
    to_char(o.fecha_operacion, 'YYYY-MM')     AS periodo_operacion,
    (o.fecha_fin_contrato - current_date)     AS dias_para_vencer,
    (o.estado = 'VIGENTE' AND o.fecha_fin_contrato IS NOT NULL
        AND o.fecha_fin_contrato BETWEEN current_date AND current_date + 30) AS vence_en_30_dias,
    -- partes
    o.cliente, pc.nombre AS cliente_nombre, pc.numero_documento AS cliente_documento,
    o.vendedor, pv.nombre AS vendedor_nombre,
    -- activo
    o.activo, a.nombre AS activo_nombre, ta.descripcion AS activo_tipo_desc,
    a.direccion AS activo_direccion, a.ubicacion, ug.nombre AS zona,
    pr.propietario, pp.nombre AS propietario_nombre,
    -- avance del cronograma
    cr.cuotas_total, cr.cuotas_cobradas, cr.cuotas_pendientes, cr.cuotas_vencidas,
    cr.monto_cronograma, cr.monto_cobrado, cr.saldo_pendiente,
    CASE WHEN COALESCE(cr.monto_cronograma, 0) > 0
         THEN round(100.0 * cr.monto_cobrado / cr.monto_cronograma, 2) END AS avance_pct
FROM operacion o
LEFT JOIN sucursal  su ON su.sucursal = o.sucursal
LEFT JOIN moneda    mo ON mo.moneda   = o.moneda
LEFT JOIN v_persona pc ON pc.persona  = o.cliente
LEFT JOIN v_persona pv ON pv.persona  = o.vendedor
LEFT JOIN activo    a  ON a.activo    = o.activo
LEFT JOIN entidad   ta ON ta.entidad  = a.tipo
LEFT JOIN ubicacion_geografica ug ON ug.ubicacion_geografica = a.ubicacion
LEFT JOIN LATERAL (SELECT x.propietario FROM activo_propietario x
                    WHERE x.activo = a.activo AND x.estado = 'ACTIVO'
                    ORDER BY x.activo_propietario LIMIT 1) pr ON true
LEFT JOIN v_persona pp ON pp.persona = pr.propietario
LEFT JOIN LATERAL (
    SELECT count(*)                                                          AS cuotas_total,
           count(*) FILTER (WHERE cc.estado = 'CANCELADO')                   AS cuotas_cobradas,
           count(*) FILTER (WHERE cc.estado = 'PENDIENTE' AND cc.saldo > 0)  AS cuotas_pendientes,
           count(*) FILTER (WHERE cc.estado = 'PENDIENTE' AND cc.saldo > 0
                              AND cc.fecha_vencimiento < current_date)       AS cuotas_vencidas,
           COALESCE(sum(cc.monto), 0)                                        AS monto_cronograma,
           COALESCE(sum(cc.monto - cc.saldo), 0)                             AS monto_cobrado,
           COALESCE(sum(cc.saldo), 0)                                        AS saldo_pendiente
      FROM cronograma_cuota cc WHERE cc.operacion = o.operacion) cr ON true;

COMMENT ON VIEW v_sabana_contrato IS 'REQ-0105 Sabana grano contrato: avance de cronograma, vencimientos y renovaciones.';
