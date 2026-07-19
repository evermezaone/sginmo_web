#!/usr/bin/env python3
"""
REQ-0106 - Construye el cubo BI en Metabase sobre las vistas v_datos_* (REQ-0105).

Idempotente: si la coleccion/pregunta/tablero ya existe (por nombre), lo actualiza en vez de duplicar.
Requiere un tunel SSH a Metabase:  ssh -N -L 3001:localhost:3001 sginmo-vps
y una API key de Metabase en la variable de entorno MB_API_KEY.

  set MB_API_KEY=mb_xxx   &&  python tools/metabase_cubo.py

Estructura (top-down):
  1. Direccion   - una pantalla: como va el negocio
  2. Gestion     - donde esta el problema (cobranza/mora, ocupacion, rentabilidad, recaudacion)
  3. Operacion   - que hago hoy (cobranza del dia, contratos por vencer)
  4. Detalle     - las 5 vistas crudas, filtrables y exportables (evidencia / drill-down)
"""
import json, os, sys, urllib.request, urllib.error

BASE = os.environ.get("MB_URL", "http://127.0.0.1:3001")
KEY = os.environ.get("MB_API_KEY", "")
DB = int(os.environ.get("MB_DB_ID", "2"))
if not KEY:
    sys.exit("Falta MB_API_KEY")


def api(path, data=None, method=None):
    r = urllib.request.Request(BASE + "/api" + path,
                               data=json.dumps(data).encode() if data is not None else None,
                               method=method or ("POST" if data is not None else "GET"))
    r.add_header("x-api-key", KEY); r.add_header("Content-Type", "application/json")
    try:
        return json.loads(urllib.request.urlopen(r, timeout=120).read().decode() or "{}")
    except urllib.error.HTTPError as e:
        print("  HTTP %s en %s: %s" % (e.code, path, e.read().decode()[:300]))
        raise


def sql_card(name, sql, display="table", viz=None, coll=None, desc=None):
    return {"name": name, "display": display, "collection_id": coll,
            "description": desc,
            "dataset_query": {"type": "native", "database": DB, "native": {"query": sql}},
            "visualization_settings": viz or {}}


# ── colecciones ──────────────────────────────────────────────────────────────
def get_or_create_collection(name, parent=None):
    for c in api("/collection"):
        if c.get("name") == name and c.get("location") == ("/%d/" % parent if parent else "/"):
            return c["id"]
    body = {"name": name, "color": "#509EE3"}
    if parent: body["parent_id"] = parent
    return api("/collection", body)["id"]


def upsert_card(card):
    existing = api("/search?q=%s&models=card" % urllib.parse.quote(card["name"]))
    for it in existing.get("data", []):
        if it.get("name") == card["name"] and it.get("collection", {}).get("id") == card["collection_id"]:
            return api("/card/%d" % it["id"], card, method="PUT")["id"]
    return api("/card", card)["id"]


def upsert_dashboard(name, coll, desc=None):
    res = api("/search?q=%s&models=dashboard" % urllib.parse.quote(name))
    for it in res.get("data", []):
        if it.get("name") == name and it.get("collection", {}).get("id") == coll:
            return it["id"]
    return api("/dashboard", {"name": name, "collection_id": coll, "description": desc})["id"]


import urllib.parse  # noqa: E402  (usado por upsert_*)

# ── SQL de las tarjetas ──────────────────────────────────────────────────────
S = {}

# --- Nivel 1: Direccion ---
S["kpi_recaudado_mes"] = """
SELECT COALESCE(sum(monto),0) AS "Recaudado del mes (Gs.)"
FROM v_datos_cobro WHERE estado_cobro='ACTIVO'
  AND fecha >= date_trunc('month', current_date) AND fecha < date_trunc('month', current_date) + interval '1 month'"""

S["kpi_cartera"] = """
SELECT COALESCE(sum(saldo),0) AS "Cartera pendiente (Gs.)" FROM v_datos_cuota WHERE saldo > 0"""

S["kpi_mora_monto"] = """
SELECT COALESCE(sum(saldo),0) AS "Saldo vencido (Gs.)" FROM v_datos_cuota WHERE esta_vencida"""

S["kpi_mora_pct"] = """
SELECT round(100.0 * COALESCE(sum(saldo) FILTER (WHERE esta_vencida),0)
            / NULLIF(sum(saldo) FILTER (WHERE saldo>0),0), 1) AS "Mora sobre cartera (%)"
FROM v_datos_cuota"""

S["kpi_ocupacion"] = """
SELECT round(100.0 * count(*) FILTER (WHERE esta_ocupado) / NULLIF(count(*) FILTER (WHERE es_alquilable),0), 1)
       AS "Ocupacion (%)" FROM v_datos_activo"""

S["kpi_vacantes"] = """
SELECT count(*) AS "Unidades vacantes alquilables"
FROM v_datos_activo WHERE es_alquilable AND NOT esta_ocupado"""

S["kpi_vacancia_costo"] = """
SELECT COALESCE(sum(precio_alquiler),0) AS "Ingreso mensual no capturado (Gs.)"
FROM v_datos_activo WHERE es_alquilable AND NOT esta_ocupado"""

S["kpi_resultado"] = """
SELECT COALESCE(sum(monto_neto),0) AS "Resultado neto acumulado (Gs.)"
FROM v_datos_movimiento WHERE estado_movimiento='CANCELADO'"""

S["g_evolucion"] = """
SELECT periodo AS "Periodo", sum(monto) AS "Cobrado"
FROM v_datos_cobro WHERE estado_cobro='ACTIVO' GROUP BY 1 ORDER BY 1"""

S["g_proyeccion"] = """
SELECT periodo_vencimiento AS "Mes", sum(saldo) AS "A cobrar"
FROM v_datos_cuota WHERE es_futura GROUP BY 1 ORDER BY 1 LIMIT 12"""

# --- Nivel 2.1: Cobranza y mora ---
S["g_aging"] = """
SELECT tramo_aging AS "Tramo", sum(saldo) AS "Saldo", count(*) AS "Cuotas"
FROM v_datos_cuota WHERE saldo > 0 GROUP BY 1
ORDER BY CASE tramo_aging WHEN 'POR VENCER' THEN 0 WHEN '01-30' THEN 1 WHEN '31-60' THEN 2
                          WHEN '61-90' THEN 3 WHEN '90+' THEN 4 ELSE 5 END"""

S["t_deudores"] = """
SELECT cliente_nombre AS "Cliente", count(*) AS "Cuotas vencidas", sum(saldo) AS "Saldo vencido",
       max(dias_mora) AS "Dias de atraso"
FROM v_datos_cuota WHERE esta_vencida GROUP BY 1 ORDER BY 3 DESC LIMIT 20"""

S["g_mora_tipo"] = """
SELECT COALESCE(activo_tipo_desc,'(sin tipo)') AS "Tipo de activo", sum(saldo) AS "Saldo vencido"
FROM v_datos_cuota WHERE esta_vencida GROUP BY 1 ORDER BY 2 DESC"""

S["t_mora_detalle"] = """
SELECT cliente_nombre AS "Cliente", activo_nombre AS "Activo", numero_cuota AS "Cuota",
       fecha_vencimiento AS "Vence", dias_mora AS "Dias", tramo_aging AS "Tramo", saldo AS "Saldo"
FROM v_datos_cuota WHERE esta_vencida ORDER BY dias_mora DESC, saldo DESC"""

# --- Nivel 2.2: Ocupacion y vacancia ---
S["g_situacion"] = """
SELECT situacion_comercial AS "Situacion", count(*) AS "Unidades"
FROM v_datos_activo GROUP BY 1 ORDER BY 2 DESC"""

S["g_ocup_tipo"] = """
SELECT COALESCE(activo_tipo_desc,'(sin tipo)') AS "Tipo",
       count(*) FILTER (WHERE esta_ocupado) AS "Ocupadas",
       count(*) FILTER (WHERE es_alquilable AND NOT esta_ocupado) AS "Vacantes"
FROM v_datos_activo GROUP BY 1 ORDER BY 2 DESC"""

S["t_vacantes"] = """
SELECT activo_nombre AS "Unidad", activo_tipo_desc AS "Tipo", COALESCE(zona,'(sin zona)') AS "Zona",
       direccion AS "Direccion", precio_alquiler AS "Alquiler pretendido",
       propietario_nombre AS "Propietario"
FROM v_datos_activo WHERE es_alquilable AND NOT esta_ocupado
ORDER BY precio_alquiler DESC NULLS LAST"""

S["t_por_vencer"] = """
SELECT activo_nombre AS "Activo", cliente_nombre AS "Inquilino", fecha_fin_contrato AS "Vence",
       dias_para_vencer AS "Dias restantes", monto_total_operacion AS "Monto contrato"
FROM v_datos_contrato
WHERE estado_operacion='VIGENTE' AND dias_para_vencer IS NOT NULL AND dias_para_vencer BETWEEN 0 AND 90
ORDER BY dias_para_vencer"""

# --- Nivel 2.3: Rentabilidad ---
S["g_ing_egr_mes"] = """
SELECT periodo AS "Periodo",
       sum(monto) FILTER (WHERE tipo_movimiento='INGRESO') AS "Ingresos",
       sum(monto) FILTER (WHERE tipo_movimiento='EGRESO')  AS "Egresos"
FROM v_datos_movimiento WHERE estado_movimiento='CANCELADO' GROUP BY 1 ORDER BY 1"""

S["g_egr_aplicacion"] = """
SELECT COALESCE(aplicacion,'(sin clasificar)') AS "Concepto", sum(monto) AS "Egreso"
FROM v_datos_movimiento WHERE tipo_movimiento='EGRESO' AND estado_movimiento='CANCELADO'
GROUP BY 1 ORDER BY 2 DESC"""

S["t_ranking_activo"] = """
SELECT COALESCE(activo_nombre,'(sin activo)') AS "Activo", sum(monto_neto) AS "Neto",
       sum(monto) FILTER (WHERE tipo_movimiento='INGRESO') AS "Ingresos",
       sum(monto) FILTER (WHERE tipo_movimiento='EGRESO')  AS "Egresos"
FROM v_datos_movimiento WHERE estado_movimiento='CANCELADO' AND NOT es_pasivo
GROUP BY 1 ORDER BY 2 ASC"""

# --- Nivel 2.4: Recaudacion ---
S["g_cobros_mes"] = """
SELECT periodo AS "Periodo", sum(monto) AS "Cobrado", count(*) AS "Cobros"
FROM v_datos_cobro WHERE estado_cobro='ACTIVO' GROUP BY 1 ORDER BY 1"""

S["g_forma_pago"] = """
SELECT COALESCE(forma_pago_desc,'(sin forma)') AS "Forma de pago", sum(monto) AS "Cobrado"
FROM v_datos_cobro WHERE estado_cobro='ACTIVO' GROUP BY 1 ORDER BY 2 DESC"""

S["g_ticket"] = """
SELECT periodo AS "Periodo", round(avg(monto)) AS "Ticket promedio"
FROM v_datos_cobro WHERE estado_cobro='ACTIVO' GROUP BY 1 ORDER BY 1"""

# --- Nivel 3: Operacion ---
S["t_cobrar_hoy"] = """
SELECT cliente_nombre AS "Cliente", activo_nombre AS "Activo", numero_cuota AS "Cuota",
       fecha_vencimiento AS "Vencio", dias_mora AS "Dias", saldo AS "A cobrar"
FROM v_datos_cuota WHERE esta_vencida ORDER BY dias_mora DESC, saldo DESC LIMIT 100"""

S["t_prox_vencer_30"] = """
SELECT activo_nombre AS "Activo", cliente_nombre AS "Inquilino", fecha_fin_contrato AS "Vence",
       dias_para_vencer AS "Dias"
FROM v_datos_contrato WHERE vence_en_30_dias ORDER BY dias_para_vencer"""

S["t_cuotas_del_mes"] = """
SELECT cliente_nombre AS "Cliente", activo_nombre AS "Activo", fecha_vencimiento AS "Vence",
       monto AS "Monto", estado_cuota AS "Estado"
FROM v_datos_cuota
WHERE fecha_vencimiento >= date_trunc('month', current_date)
  AND fecha_vencimiento <  date_trunc('month', current_date) + interval '1 month'
ORDER BY fecha_vencimiento"""

# --- Nivel 4: Detalle ---
DETALLE = [("Cuotas (detalle)", "v_datos_cuota"), ("Cobros (detalle)", "v_datos_cobro"),
           ("Activos (detalle)", "v_datos_activo"), ("Movimientos (detalle)", "v_datos_movimiento"),
           ("Contratos (detalle)", "v_datos_contrato")]

MONEY = {"column_settings": {}}


def main():
    print("== colecciones ==")
    root = get_or_create_collection("SGInmo BI")
    c1 = get_or_create_collection("1. Direccion", root)
    c2 = get_or_create_collection("2. Gestion", root)
    c3 = get_or_create_collection("3. Operacion", root)
    c4 = get_or_create_collection("4. Detalle", root)
    print("  raiz=%s  niveles=%s" % (root, [c1, c2, c3, c4]))

    ids = {}

    def card(key, name, coll, display="table", viz=None, desc=None):
        ids[key] = upsert_card(sql_card(name, S[key], display, viz, coll, desc))
        print("  card %-22s -> %s" % (key, ids[key]))

    print("== nivel 1: Direccion ==")
    card("kpi_recaudado_mes", "Recaudado del mes", c1, "scalar")
    card("kpi_cartera", "Cartera pendiente", c1, "scalar")
    card("kpi_mora_monto", "Saldo vencido", c1, "scalar")
    card("kpi_mora_pct", "Mora sobre cartera %", c1, "scalar")
    card("kpi_ocupacion", "Ocupacion %", c1, "scalar")
    card("kpi_vacantes", "Unidades vacantes", c1, "scalar")
    card("kpi_vacancia_costo", "Ingreso mensual no capturado", c1, "scalar",
         desc="Suma del alquiler pretendido de las unidades alquilables hoy vacantes.")
    card("kpi_resultado", "Resultado neto acumulado", c1, "scalar")
    card("g_evolucion", "Evolucion de la recaudacion", c1, "bar")
    card("g_proyeccion", "Proyeccion de cobranza", c1, "bar",
         desc="Cuotas futuras por mes de vencimiento. El sistema no tenia esta vista.")

    print("== nivel 2: Gestion ==")
    card("g_aging", "Aging de cartera", c2, "bar")
    card("t_deudores", "Top deudores", c2, "table")
    card("g_mora_tipo", "Mora por tipo de activo", c2, "bar")
    card("t_mora_detalle", "Cartera vencida (detalle)", c2, "table")
    card("g_situacion", "Situacion del parque", c2, "pie")
    card("g_ocup_tipo", "Ocupacion por tipo", c2, "bar")
    card("t_vacantes", "Vacantes alquilables", c2, "table",
         desc="Cada fila es alquiler que hoy no se cobra.")
    card("t_por_vencer", "Contratos por vencer (90 dias)", c2, "table")
    card("g_ing_egr_mes", "Ingresos vs egresos por mes", c2, "bar")
    card("g_egr_aplicacion", "Egresos por concepto", c2, "bar")
    card("t_ranking_activo", "Ranking de activos por neto", c2, "table",
         desc="Ordenado de peor a mejor: arriba, los que hoy restan.")
    card("g_cobros_mes", "Cobros por mes", c2, "bar")
    card("g_forma_pago", "Cobros por forma de pago", c2, "pie")
    card("g_ticket", "Ticket promedio por mes", c2, "line")

    print("== nivel 3: Operacion ==")
    card("t_cobrar_hoy", "A cobrar hoy (vencidas)", c3, "table")
    card("t_prox_vencer_30", "Contratos a renovar (30 dias)", c3, "table")
    card("t_cuotas_del_mes", "Cuotas del mes en curso", c3, "table")

    print("== nivel 4: Detalle ==")
    for nombre, vista in DETALLE:
        k = "det_" + vista
        S[k] = "SELECT * FROM %s" % vista
        card(k, nombre, c4, "table", desc="Vista completa %s, filtrable y exportable a Excel/CSV." % vista)

    # ── tableros ─────────────────────────────────────────────────────────────
    print("== tableros ==")

    def build(nombre, coll, layout, desc=None):
        did = upsert_dashboard(nombre, coll, desc)
        dash = api("/dashboard/%d" % did)
        dashcards = []
        for i, (key, row, col, w, h) in enumerate(layout):
            dashcards.append({"id": -(i + 1), "card_id": ids[key], "row": row, "col": col,
                              "size_x": w, "size_y": h,
                              "parameter_mappings": [], "visualization_settings": {}})
        api("/dashboard/%d" % did, {"dashcards": dashcards}, method="PUT")
        print("  tablero %-42s -> %s (%d tarjetas)" % (nombre, did, len(dashcards)))
        return did

    build("1. Direccion - Como va el negocio", c1, [
        ("kpi_recaudado_mes", 0, 0, 6, 3), ("kpi_cartera", 0, 6, 6, 3),
        ("kpi_mora_monto", 0, 12, 6, 3), ("kpi_mora_pct", 0, 18, 6, 3),
        ("kpi_ocupacion", 3, 0, 6, 3), ("kpi_vacantes", 3, 6, 6, 3),
        ("kpi_vacancia_costo", 3, 12, 6, 3), ("kpi_resultado", 3, 18, 6, 3),
        ("g_evolucion", 6, 0, 12, 6), ("g_proyeccion", 6, 12, 12, 6),
    ], "Una pantalla para saber si hay que preocuparse.")

    build("2.1 Cobranza y mora", c2, [
        ("g_aging", 0, 0, 12, 6), ("g_mora_tipo", 0, 12, 12, 6),
        ("t_deudores", 6, 0, 12, 7), ("t_mora_detalle", 6, 12, 12, 7),
    ], "Donde esta la plata que no entra.")

    build("2.2 Ocupacion y vacancia", c2, [
        ("g_situacion", 0, 0, 8, 6), ("g_ocup_tipo", 0, 8, 16, 6),
        ("t_vacantes", 6, 0, 12, 7), ("t_por_vencer", 6, 12, 12, 7),
    ], "Que unidades no producen y que contratos se caen.")

    build("2.3 Rentabilidad", c2, [
        ("g_ing_egr_mes", 0, 0, 24, 6),
        ("g_egr_aplicacion", 6, 0, 12, 6), ("t_ranking_activo", 6, 12, 12, 6),
    ], "Que deja y que cuesta.")

    build("2.4 Recaudacion", c2, [
        ("g_cobros_mes", 0, 0, 16, 6), ("g_forma_pago", 0, 16, 8, 6),
        ("g_ticket", 6, 0, 24, 5),
    ], "Como viene entrando la plata.")

    build("3. Operacion - Que hago hoy", c3, [
        ("t_cobrar_hoy", 0, 0, 14, 8), ("t_prox_vencer_30", 0, 14, 10, 8),
        ("t_cuotas_del_mes", 8, 0, 24, 7),
    ], "La lista de tareas del dia.")

    print("\nLISTO. Entra por http://<host>:3001 -> coleccion 'SGInmo BI'.")


if __name__ == "__main__":
    main()
