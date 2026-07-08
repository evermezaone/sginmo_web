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

def map_persona(fila, origen=None):
    """CLIENTES/PROVEEDORES del legado -> persona COMPLETA (obs 239):
    base + especializacion (persona_fisica/persona_juridica) + roles.
    Clave natural: numero_documento."""
    doc = str(fila.get("RUC") or fila.get("CEDULA") or fila.get("NRO_DOCUMENTO") or "").strip()
    if not doc:
        return None
    es_juridica = bool(fila.get("RAZON_SOCIAL"))
    nombre = (fila.get("RAZON_SOCIAL") or fila.get("NOMBRE") or "").strip()
    base = {
        "tipo_personeria": "JURIDICA" if es_juridica else "FISICA",
        "nombre": nombre,
        "numero_documento": doc,
        "es_contribuyente": bool(fila.get("RUC")),
        "direccion": fila.get("DIRECCION"),
        "telefono": fila.get("TELEFONO"),
        "email": fila.get("EMAIL"),
        "estado": "ACTIVO",
    }
    if es_juridica:
        espec = {"tabla": "persona_juridica",
                 "datos": {"razon_social": nombre,
                           "nombre_fantasia": (fila.get("NOMBRE_FANTASIA") or None)}}
    else:
        partes = nombre.split(" ", 1)
        espec = {"tabla": "persona_fisica",
                 "datos": {"nombres": (fila.get("NOMBRES") or partes[0] or nombre),
                           "apellidos": (fila.get("APELLIDOS") or (partes[1] if len(partes) > 1 else "-"))}}
    # rol segun la tabla legada de origen
    roles = []
    o = (origen or "").upper()
    if o.startswith("CLIENTE"):
        roles.append("CLIENTE")
    elif o.startswith("PROVEEDOR"):
        roles.append("PROVEEDOR")
    return {"persona": base, "especializacion": espec, "roles": roles}

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
    "personas": {"origen": ["CLIENTES", "PROVEEDORES", "PERSONAS"], "destino": "persona (+fisica/juridica/rol)",
                 "fn": map_persona, "cargador": "personas"},
    "activos":  {"origen": ["INMUEBLES", "PROPIEDADES", "ENTIDADES_INMOBILIARIAS"], "destino": "activo",
                 "fn": map_activo, "cargador": "activos"},
    # ── Familias transaccionales (obs 239): pasos declarativos EXPLICITAMENTE NO
    # destructivos. Se registran en el pipeline y reportan que haria cada uno, pero
    # NUNCA escriben hasta que el .fdb de produccion revele el esquema real y se
    # complete su fn. El orden respeta las dependencias del mapeo del docstring.
    "operaciones": {"origen": ["CONTRATOS", "OPERACIONES"], "destino": "operacion (+documento cta cte)",
                    "stub": "requiere esquema real del .fdb: mapear cliente/activo por clave natural y"
                            " crear via SQL (persona/activo ya migrados)"},
    "cuotas":      {"origen": ["CUOTAS", "CRONOGRAMA"], "destino": "cronograma_cuota",
                    "stub": "requiere esquema real: preferir regenerar con f_generar_cronograma para"
                            " garantizar el cuadre del motor"},
    "cobros":      {"origen": ["RECIBOS", "COBROS"], "destino": "cobro + cobro_detalle",
                    "stub": "requiere esquema real: cargar VIA f_cobrar_documento para que los triggers"
                            " cuadren saldos y cuotas (jamas INSERT directo)"},
    "gastos":      {"origen": ["GASTOS", "INGRESOS_EGRESOS"], "destino": "ingreso_egreso",
                    "stub": "requiere esquema real: mapear articulo por aplicacion y persona por documento"},
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
    con = None
    try:
        for nombre, g in grupos.items():
            if "stub" in g:
                filas = sum(len(origen.get(ot, [])) for ot in g["origen"])
                print(f"  {nombre}: STUB NO DESTRUCTIVO ({filas} filas legadas detectadas; no escribe)."
                      f" Destino {g['destino']}. Pendiente: {g['stub']}")
                continue
            # mapear conservando la tabla de origen (define el rol de la persona)
            mapeadas = []
            for ot in g["origen"]:
                for f in origen.get(ot, []):
                    m = g["fn"](f, ot) if g["cargador"] == "personas" else g["fn"](f)
                    if m is not None:
                        mapeadas.append(m)
            total += len(mapeadas)
            print(f"  {nombre}: {len(mapeadas)} a cargar en {g['destino']}")
            if args.apply and mapeadas:
                if con is None:
                    con = _conectar_pg(env)
                cur = con.cursor()
                nuevos, existentes = (_cargar_personas(cur, mapeadas) if g["cargador"] == "personas"
                                      else _cargar_activos(cur, mapeadas))
                con.commit()
                print(f"    -> insertados {nuevos}, ya existian {existentes} (idempotente por clave natural)")
    finally:
        if con is not None:
            con.close()
    print(f"=== {'Simulacion' if args.dry_run else 'Carga'} completada: {total} filas ===")
    if total == 0:
        print("  (0 filas: el .fdb legado esta vacio o LEGACY_FDB_PATH sin configurar — esperado hoy)")

def _conectar_pg(env):
    import psycopg2
    return psycopg2.connect(host=env["APP_DB_HOST"], port=env.get("APP_DB_PORT", 5432),
                            user=env["APP_DB_USER"], password=env["APP_DB_PASS"], dbname=env["APP_DB_NAME"])

def _cargar_personas(cur, items):
    """persona base + especializacion + roles (obs 239). Idempotente por numero_documento:
    el lookup en Python decide insertar o reutilizar el id (obs 240: nada de ON CONFLICT
    ciego sobre claves que no son UNIQUE)."""
    nuevos = existentes = 0
    for it in items:
        p, espec, roles = it["persona"], it["especializacion"], it["roles"]
        cur.execute("SELECT persona FROM persona WHERE numero_documento = %s", (p["numero_documento"],))
        r = cur.fetchone()
        if r:
            pid = r[0]
            existentes += 1
        else:
            cols = list(p.keys())
            cur.execute(
                f"INSERT INTO persona ({', '.join(cols)}, usuario_creacion, fecha_creacion)"
                f" VALUES ({', '.join(['%s'] * len(cols))}, 'etl', now()) RETURNING persona",
                list(p.values()))
            pid = cur.fetchone()[0]
            nuevos += 1
        # especializacion: PK compartida (persona) => upsert seguro por PK real
        e_cols = ["persona"] + list(espec["datos"].keys())
        e_vals = [pid] + list(espec["datos"].values())
        cur.execute(
            f"INSERT INTO {espec['tabla']} ({', '.join(e_cols)}, usuario_creacion, fecha_creacion)"
            f" VALUES ({', '.join(['%s'] * len(e_cols))}, 'etl', now())"
            f" ON CONFLICT (persona) DO NOTHING", e_vals)
        # roles: lookup explicito (persona, rol_codigo) — reactiva si quedo INACTIVO
        for rol in roles:
            cur.execute("SELECT persona_rol, estado FROM persona_rol WHERE persona = %s AND rol_codigo = %s",
                        (pid, rol))
            pr = cur.fetchone()
            if pr is None:
                cur.execute(
                    "INSERT INTO persona_rol (persona, rol_codigo, estado, usuario_creacion, fecha_creacion)"
                    " VALUES (%s, %s, 'ACTIVO', 'etl', now())", (pid, rol))
            elif pr[1] != "ACTIVO":
                cur.execute("UPDATE persona_rol SET estado = 'ACTIVO', usuario_modificacion = 'etl',"
                            " fecha_modificacion = now() WHERE persona_rol = %s", (pr[0],))
    return nuevos, existentes

def _cargar_activos(cur, items):
    """activo idempotente por clave natural nombre+tipo_codigo VIA LOOKUP en Python
    (obs 240: la tabla no tiene UNIQUE(nombre, tipo_codigo), asi que ON CONFLICT
    DO NOTHING jamas dispararia y re-correr duplicaria)."""
    nuevos = existentes = 0
    for a in items:
        cur.execute("SELECT activo FROM activo WHERE nombre = %s AND tipo_codigo = %s",
                    (a["nombre"], a["tipo_codigo"]))
        if cur.fetchone():
            existentes += 1
            continue
        cols = list(a.keys())
        cur.execute(
            f"INSERT INTO activo ({', '.join(cols)}, usuario_creacion, fecha_creacion)"
            f" VALUES ({', '.join(['%s'] * len(cols))}, 'etl', now())", list(a.values()))
        nuevos += 1
    return nuevos, existentes

if __name__ == "__main__":
    sys.exit(main())
