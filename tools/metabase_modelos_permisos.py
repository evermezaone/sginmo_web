#!/usr/bin/env python3
"""
REQ-0106 (fase 2) - Analisis dinamico y control de acceso en Metabase.

Resuelve dos cosas que el cubo de tarjetas SQL no cubre:

1) ANALISIS DINAMICO: expone las 5 vistas v_datos_* como MODELOS de Metabase. Un modelo es un punto
   de partida explorable: el usuario lo abre y puede agrupar, filtrar, resumir y PIVOTEAR sin escribir
   SQL, con drill-down por clic. Sobre SQL nativo eso no se puede; sobre un modelo si.
   Ademas crea tablas dinamicas (pivot) de ejemplo, ya armadas.

2) ACCESO POR PERFIL: crea los grupos y fija permisos por coleccion, para que un usuario operativo
   entre solo a lo suyo y no vea (ni pueda tocar) el resto.

   Grupos:
     - "Gerencia"  : ve todo (Direccion, Gestion, Operacion, Detalle) + puede explorar y crear.
     - "Operativo" : ve solo "3. Operacion" y "4. Detalle"; puede explorar pero no editar el cubo.
     - "Consulta"  : solo lectura de "1. Direccion"; sin acceso al detalle ni a SQL.

   Las CUENTAS de usuario no se crean aca a proposito: las crea el administrador desde
   Administracion > Personas (cada quien define su propia contrasena).

Uso:  set MB_API_KEY=mb_xxx  &&  python tools/metabase_modelos_permisos.py
Requiere tunel:  ssh -N -L 3001:localhost:3001 sginmo-vps
"""
import json, os, sys, urllib.parse, urllib.request, urllib.error

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
        body = e.read().decode()[:300]
        print("  HTTP %s en %s: %s" % (e.code, path, body))
        return {"__err": e.code, "__body": body}


def find_collection(name):
    for c in api("/collection"):
        if c.get("name") == name:
            return c["id"]
    return None


def find_card(name, coll):
    res = api("/search?q=%s&models=card&models=dataset" % urllib.parse.quote(name))
    for it in res.get("data", []):
        if it.get("name") == name and (it.get("collection") or {}).get("id") == coll:
            return it["id"]
    return None


# ── 1) modelos: las 5 vistas como punto de partida explorable ────────────────
MODELOS = [
    ("Cuotas", "v_datos_cuota",
     "Una fila por cuota: vencimiento, monto, saldo, estado, aging, cliente, activo, zona. "
     "Base para mora, cobranza esperada y flujo proyectado."),
    ("Cobros", "v_datos_cobro",
     "Una fila por cobro: fecha, monto, forma de pago, cliente, sucursal, activo alcanzado."),
    ("Activos", "v_datos_activo",
     "Una fila por inmueble: situacion comercial, precio, ocupacion, contrato vigente, propietario."),
    ("Movimientos", "v_datos_movimiento",
     "Una fila por ingreso/egreso: concepto, monto neto, activo, operacion, periodo."),
    ("Contratos", "v_datos_contrato",
     "Una fila por contrato: plazo, montos, avance del cronograma, dias para vencer."),
]


def crear_modelos(coll):
    ids = {}
    for nombre, vista, desc in MODELOS:
        body = {"name": nombre, "type": "model", "collection_id": coll, "description": desc,
                "display": "table", "visualization_settings": {},
                "dataset_query": {"type": "native", "database": DB,
                                  "native": {"query": "SELECT * FROM %s" % vista}}}
        ex = find_card(nombre, coll)
        r = api("/card/%d" % ex, body, method="PUT") if ex else api("/card", body)
        if "__err" in r:
            print("  modelo %-12s FALLO" % nombre); continue
        ids[vista] = r["id"]
        print("  modelo %-12s -> card %s (%s)" % (nombre, r["id"], vista))
    return ids


# ── 2) tablas dinamicas (pivot) de ejemplo ───────────────────────────────────
def crear_pivots(coll):
    pivots = [
        ("Pivot - Cartera por tipo y tramo de aging",
         """SELECT COALESCE(activo_tipo_desc,'(sin tipo)') AS "Tipo de activo",
                   tramo_aging AS "Tramo", sum(saldo) AS "Saldo"
            FROM v_datos_cuota WHERE saldo > 0 GROUP BY 1,2""",
         {"pivot_table.column_split": {"rows": ["Tipo de activo"], "columns": ["Tramo"],
                                       "values": ["Saldo"]}},
         "Cruce clasico: que tipo de inmueble concentra la mora."),
        ("Pivot - Recaudacion por mes y forma de pago",
         """SELECT periodo AS "Periodo", COALESCE(forma_pago_desc,'(sin forma)') AS "Forma",
                   sum(monto) AS "Cobrado"
            FROM v_datos_cobro WHERE estado_cobro='ACTIVO' GROUP BY 1,2""",
         {"pivot_table.column_split": {"rows": ["Periodo"], "columns": ["Forma"],
                                       "values": ["Cobrado"]}},
         "Como entra la plata mes a mes y por que medio."),
        ("Pivot - Ocupacion por zona y tipo",
         """SELECT COALESCE(zona,'(sin zona)') AS "Zona",
                   COALESCE(activo_tipo_desc,'(sin tipo)') AS "Tipo",
                   count(*) AS "Unidades"
            FROM v_datos_activo GROUP BY 1,2""",
         {"pivot_table.column_split": {"rows": ["Zona"], "columns": ["Tipo"],
                                       "values": ["Unidades"]}},
         "Distribucion del parque por zona geografica y tipo."),
    ]
    out = []
    for nombre, sql, viz, desc in pivots:
        body = {"name": nombre, "display": "pivot", "collection_id": coll, "description": desc,
                "visualization_settings": viz,
                "dataset_query": {"type": "native", "database": DB, "native": {"query": sql}}}
        ex = find_card(nombre, coll)
        r = api("/card/%d" % ex, body, method="PUT") if ex else api("/card", body)
        if "__err" not in r:
            out.append(r["id"]); print("  pivot %-46s -> %s" % (nombre[:46], r["id"]))
    return out


# ── 3) grupos y permisos ─────────────────────────────────────────────────────
def get_or_create_group(nombre):
    for g in api("/permissions/group"):
        if g.get("name") == nombre:
            return g["id"]
    r = api("/permissions/group", {"name": nombre})
    return r.get("id")


def aplicar_permisos(colls, grupos):
    """colls: dict nombre->id ; grupos: dict nombre->id"""
    graph = api("/collection/graph")
    if "__err" in graph:
        print("  no se pudo leer el grafo de colecciones"); return
    g = graph["groups"]

    def set_perm(gid, cid, perm):
        g.setdefault(str(gid), {})[str(cid)] = perm   # "write" | "read" | "none"

    for nombre, gid in grupos.items():
        for cn, cid in colls.items():
            if nombre == "Gerencia":
                set_perm(gid, cid, "write")
            elif nombre == "Operativo":
                set_perm(gid, cid, "read" if cn in ("3. Operacion", "4. Detalle", "SGInmo BI") else "none")
            elif nombre == "Consulta":
                set_perm(gid, cid, "read" if cn in ("1. Direccion", "SGInmo BI") else "none")
    r = api("/collection/graph", {"revision": graph["revision"], "groups": g}, method="PUT")
    print("  permisos de coleccion aplicados" if "__err" not in r else "  fallo al aplicar permisos")


def main():
    root = find_collection("SGInmo BI")
    colls = {n: find_collection(n) for n in
             ["SGInmo BI", "1. Direccion", "2. Gestion", "3. Operacion", "4. Detalle"]}
    colls = {k: v for k, v in colls.items() if v}
    print("colecciones:", colls)

    # los modelos y pivots viven en una coleccion propia, visible para quien explore
    explor = None
    for c in api("/collection"):
        if c.get("name") == "5. Analisis libre":
            explor = c["id"]; break
    if not explor:
        explor = api("/collection", {"name": "5. Analisis libre", "parent_id": root,
                                     "color": "#88BF4D"})["id"]
    colls["5. Analisis libre"] = explor
    print("coleccion de analisis libre:", explor)

    print("== modelos (base para pivotear sin SQL) ==")
    crear_modelos(explor)
    print("== tablas dinamicas de ejemplo ==")
    crear_pivots(explor)

    print("== grupos ==")
    grupos = {n: get_or_create_group(n) for n in ["Gerencia", "Operativo", "Consulta"]}
    print("  ", grupos)

    print("== permisos por coleccion ==")
    aplicar_permisos(colls, grupos)

    print("\nLISTO.")
    print("  - Modelos y pivots en la coleccion '5. Analisis libre'.")
    print("  - Grupos creados. Las CUENTAS las crea el admin en Administracion > Personas,")
    print("    asignando cada persona a Gerencia / Operativo / Consulta.")


if __name__ == "__main__":
    main()
