#!/usr/bin/env python3
"""
ETL Firebird (INMOBILIARIA.FDB, legado SGInmo) -> PostgreSQL (sginmo web) — REQ-0031.

Proceso de GO-LIVE (no runtime): lee el legado Firebird 2.5 y carga las tablas nuevas
respetando el orden de dependencias y el mapeo documentado. Idempotente por clave natural
(numero_documento de persona, nombre+tipo de activo, etc.) para poder re-correrse.

Uso:
  python tools/etl_firebird_postgres.py --dry-run          # solo reporta que haria
  python tools/etl_firebird_postgres.py --apply            # ejecuta la carga
  python tools/etl_firebird_postgres.py --apply --tabla personas

Requisitos: fdb (driver Firebird) o el Firebird embebido portable del scratchpad;
psycopg2 para PostgreSQL. Credenciales del .env (APP_DB_* para PostgreSQL destino,
LEGACY_FDB_PATH para el archivo .fdb origen).

MAPEO (legado Firebird -> nuevo PostgreSQL), en orden de dependencia:
  1. CLIENTES/PROVEEDORES/PERSONAS  -> persona (+ persona_fisica/juridica) + persona_rol
  2. INMUEBLES/PROPIEDADES/ENTIDADES -> activo (recursivo; tipo por clasificacion legada)
  3. CONTRATOS/OPERACIONES           -> operacion (+ documento cta cte)
  4. CUOTAS/CRONOGRAMA               -> cronograma_cuota (o regenerar con f_generar_cronograma)
  5. RECIBOS/COBROS                  -> cobro + cobro_detalle (via f_cobrar_documento, para
                                        que los triggers cuadren el saldo)
  6. GASTOS/INGRESOS                 -> ingreso_egreso

NOTA: la BD legada tiene POCOS o NINGUN dato (el usuario priorizo migrar la LOGICA del
codigo, no los datos). Por eso el ETL se entrega listo y en dry-run; se corre con --apply
cuando el usuario provea el .fdb de produccion final. Las estructuras exactas de las tablas
legadas se resuelven en tiempo de ejecucion leyendo RDB$RELATIONS del Firebird.
"""
import argparse
import os
import sys

def cargar_env():
    env = {}
    ruta = os.path.join(os.path.dirname(__file__), "..", ".env")
    if os.path.exists(ruta):
        for linea in open(ruta, encoding="utf-8"):
            linea = linea.strip()
            if linea and not linea.startswith("#") and "=" in linea:
                k, v = linea.split("=", 1)
                env[k.strip()] = v.strip()
    return env

# ── Mapeos declarativos: tabla legada -> (tabla nueva, funcion de fila) ──
# Cada funcion recibe una fila legada (dict) y devuelve un dict para el INSERT nuevo,
# o None para omitir. Se completan al conocer el esquema real del .fdb de produccion.

def map_persona(fila):
    """CLIENTES/PROVEEDORES del legado -> persona. Clave natural: numero_documento."""
    doc = str(fila.get("RUC") or fila.get("CEDULA") or fila.get("NRO_DOCUMENTO") or "").strip()
    if not doc:
        return None
    es_juridica = bool(fila.get("RAZON_SOCIAL"))
    return {
        "tipo_personeria": "JURIDICA" if es_juridica else "FISICA",
        "nombre": (fila.get("RAZON_SOCIAL") or fila.get("NOMBRE") or "").strip(),
        "numero_documento": doc,
        "es_contribuyente": bool(fila.get("RUC")),
        "direccion": fila.get("DIRECCION"),
        "telefono": fila.get("TELEFONO"),
        "email": fila.get("EMAIL"),
        "estado": "ACTIVO",
    }

def map_activo(fila):
    """INMUEBLES/PROPIEDADES del legado -> activo. Clave natural: nombre + tipo."""
    nombre = (fila.get("DESCRIPCION") or fila.get("NOMBRE") or "").strip()
    if not nombre:
        return None
    return {
        "nombre": nombre,
        "tipo_codigo": _tipo_activo(fila.get("TIPO") or fila.get("CLASIFICACION")),
        "direccion": fila.get("DIRECCION"),
        "precio_venta": fila.get("PRECIO_VENTA") or 0,
        "precio_alquiler": fila.get("PRECIO_ALQUILER") or 0,
        "estado": _estado_activo(fila.get("ESTADO")),
    }

def _tipo_activo(t):
    m = {"CASA": "CASA", "DEPARTAMENTO": "DEPARTAMENTO", "DEPTO": "DEPARTAMENTO",
         "TERRENO": "TERRENO", "LOTE": "LOTE", "EDIFICIO": "EDIFICIO", "LOCAL": "LOCAL"}
    return m.get(str(t or "").upper(), "CASA")

def _estado_activo(e):
    m = {"L": "LIBRE", "LIBRE": "LIBRE", "O": "OCUPADA", "OCUPADA": "OCUPADA",
         "ALQUILADA": "OCUPADA", "V": "VENDIDA", "VENDIDA": "VENDIDA"}
    return m.get(str(e or "").upper(), "LIBRE")

TABLAS = {
    "personas": {"origen": ["CLIENTES", "PROVEEDORES", "PERSONAS"], "destino": "persona", "fn": map_persona},
    "activos":  {"origen": ["INMUEBLES", "PROPIEDADES", "ENTIDADES_INMOBILIARIAS"], "destino": "activo", "fn": map_activo},
    # operaciones/cuotas/cobros/gastos: se completan con el esquema real del .fdb de produccion.
}

def leer_firebird(env, tablas_origen):
    """Lee las tablas legadas. Devuelve {tabla: [filas...]}. Requiere driver fdb + .fdb."""
    try:
        import fdb  # noqa
    except ImportError:
        print("  [aviso] driver 'fdb' no instalado; usar Firebird embebido portable o pip install fdb")
        return {}
    ruta = env.get("LEGACY_FDB_PATH")
    if not ruta or not os.path.exists(ruta):
        print(f"  [aviso] LEGACY_FDB_PATH no configurado o inexistente: {ruta}")
        return {}
    con = fdb.connect(dsn=ruta, user=env.get("LEGACY_FDB_USER", "SYSDBA"),
                      password=env.get("LEGACY_FDB_PASS", "masterkey"))
    resultado = {}
    cur = con.cursor()
    # tablas reales del legado
    cur.execute("SELECT TRIM(RDB$RELATION_NAME) FROM RDB$RELATIONS WHERE RDB$SYSTEM_FLAG=0")
    reales = {r[0].upper() for r in cur.fetchall()}
    for t in tablas_origen:
        if t.upper() in reales:
            cur.execute(f"SELECT * FROM {t}")
            cols = [d[0].upper() for d in cur.description]
            resultado[t] = [dict(zip(cols, row)) for row in cur.fetchall()]
    con.close()
    return resultado

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--dry-run", action="store_true")
    ap.add_argument("--apply", action="store_true")
    ap.add_argument("--tabla", default=None)
    args = ap.parse_args()
    if not args.dry_run and not args.apply:
        args.dry_run = True
    env = cargar_env()

    grupos = {args.tabla: TABLAS[args.tabla]} if args.tabla else TABLAS
    print(f"=== ETL Firebird -> PostgreSQL ({'DRY-RUN' if args.dry_run else 'APPLY'}) ===")
    origen = leer_firebird(env, [t for g in grupos.values() for t in g["origen"]])
    total = 0
    for nombre, g in grupos.items():
        filas = []
        for ot in g["origen"]:
            filas += origen.get(ot, [])
        mapeadas = [m for m in (g["fn"](f) for f in filas) if m is not None]
        total += len(mapeadas)
        print(f"  {nombre}: {len(filas)} legadas -> {len(mapeadas)} a cargar en {g['destino']}")
        if args.apply and mapeadas:
            _insertar(env, g["destino"], mapeadas)
    print(f"=== {'Simulacion' if args.dry_run else 'Carga'} completada: {total} filas ===")
    if total == 0:
        print("  (0 filas: el .fdb legado esta vacio o LEGACY_FDB_PATH sin configurar — esperado hoy)")

def _insertar(env, tabla, filas):
    import psycopg2
    con = psycopg2.connect(host=env["APP_DB_HOST"], port=env.get("APP_DB_PORT", 5432),
                           user=env["APP_DB_USER"], password=env["APP_DB_PASS"], dbname=env["APP_DB_NAME"])
    cur = con.cursor()
    for f in filas:
        cols = list(f.keys()) + ["usuario_creacion", "fecha_creacion"]
        vals = list(f.values()) + ["etl", "now()"]
        ph = ", ".join(["%s"] * (len(cols) - 1)) + ", now()"
        cur.execute(f"INSERT INTO {tabla} ({', '.join(cols)}) VALUES ({ph}) ON CONFLICT DO NOTHING",
                    vals[:-1])
    con.commit()
    con.close()

if __name__ == "__main__":
    sys.exit(main())
