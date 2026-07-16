#!/usr/bin/env python3
"""
REQ-0103 - Verificacion reproducible legado (Firebird) vs web (PostgreSQL tenant 1).
Imprime la tabla de cuadre y el chequeo por-cuota (estado + fecha_cancelacion). Exit != 0 si algo no cuadra.

  python tools/verifica_0103.py        # requiere tunel SSH 127.0.0.1:15432 abierto
"""
import os, sys, json

SP = r"C:/Users/everm/AppData/Local/Temp/claude/C--Users-everm-OneDrive-Documents-Datos-claude-semaforo-semaforo-adaptivo-desarrollo/a89655ce-0ba4-4159-82b6-6f74ec57e4a3/scratchpad"
FB = SP + "/fb/fb25"
os.environ["FIREBIRD"] = FB
os.environ["PATH"] = FB + os.pathsep + os.environ.get("PATH", "")
import fdb; fdb.load_api(FB + "/fbembed.dll")
import psycopg2

FDB = r"C:/Users/everm/OneDrive/Documents/Datos/Sistemas/2R/Desarrollo/SGInmo/codigo fuente/inmobiliaria/Pysistemas/migracion/source/INMOBILIARIA.FDB"

def env_pw():
    for l in open(os.path.join(os.path.dirname(__file__), "..", ".env"), encoding="utf-8"):
        if l.startswith("APP_DB_PASS="):
            return l.split("=", 1)[1].strip()

fb = fdb.connect(database=FDB, user="SYSDBA", password="masterkey", charset="WIN1252"); fc = fb.cursor()
pg = psycopg2.connect(host="127.0.0.1", port=15432, dbname="sginmo", user="sginmo", password=env_pw(), connect_timeout=15)
pc = pg.cursor(); pc.execute("SET app.tenant='-1'")
def L(s): fc.execute(s); return fc.fetchone()[0]
def W(s): pc.execute(s); return pc.fetchone()[0]

fails = 0
def chk(nombre, leg, web):
    global fails
    ok = (leg == web); fails += (0 if ok else 1)
    print("  %-26s legado=%-16s web=%-16s %s" % (nombre, format(leg, ",d"), format(web, ",d"), "OK" if ok else "*** DIFERENCIA ***"))

print("== CUADRE LEGADO vs WEB (tenant 1) ==")
chk("activos", L("SELECT (SELECT COUNT(*) FROM ENTIDADES_INMOBILIARIAS)+(SELECT COUNT(*) FROM PROPIEDADES) FROM RDB$DATABASE"),
    W("SELECT count(*) FROM activo WHERE tenant=1"))
chk("operaciones", L("SELECT COUNT(*) FROM OPERACIONES_PROPIEDADES"), W("SELECT count(*) FROM operacion WHERE tenant=1"))
chk("cuotas", L("SELECT COUNT(*) FROM CRONOGRAMAS_CUOTAS"),
    W("SELECT count(*) FROM cronograma_cuota cc JOIN operacion o ON o.operacion=cc.operacion WHERE o.tenant=1"))
chk("suma cuotas", int(L("SELECT SUM(MONTO) FROM CRONOGRAMAS_CUOTAS")),
    int(W("SELECT sum(cc.monto) FROM cronograma_cuota cc JOIN operacion o ON o.operacion=cc.operacion WHERE o.tenant=1")))
chk("cuotas canceladas", L("SELECT COUNT(*) FROM CRONOGRAMAS_CUOTAS WHERE ESTADO='CANCELADO'"),
    W("SELECT count(*) FROM cronograma_cuota cc JOIN operacion o ON o.operacion=cc.operacion WHERE o.tenant=1 AND cc.estado='CANCELADO'"))
chk("saldo por cobrar", int(L("SELECT SUM(MONTO) FROM CRONOGRAMAS_CUOTAS WHERE ESTADO<>'CANCELADO'")),
    int(W("SELECT sum(cc.saldo) FROM cronograma_cuota cc JOIN operacion o ON o.operacion=cc.operacion WHERE o.tenant=1")))
chk("recaudado (cobros)", int(L("SELECT SUM(MONTO) FROM CRONOGRAMAS_CUOTAS WHERE ESTADO='CANCELADO'")),
    int(W("SELECT COALESCE(sum(monto),0) FROM cobro WHERE tenant=1")))
chk("ingresos/egresos", L("SELECT COUNT(*) FROM INGRESOS_EGRESOS"), W("SELECT count(*) FROM ingreso_egreso WHERE tenant=1"))

# cobros por mes (legado por FECHA_CANCELACION / vencimiento cuando falta) vs web por cobro.fecha
print("== COBROS POR MES (legado vs web) ==")
fc.execute("SELECT EXTRACT(YEAR FROM COALESCE(FECHA_CANCELACION,FECHA_VENCIMIENTO))||'-'||LPAD(EXTRACT(MONTH FROM COALESCE(FECHA_CANCELACION,FECHA_VENCIMIENTO)),2,'0'), SUM(MONTO) "
           "FROM CRONOGRAMAS_CUOTAS WHERE ESTADO='CANCELADO' GROUP BY 1 ORDER BY 1")
legm = {r[0]: int(r[1]) for r in fc.fetchall()}
pc.execute("SELECT to_char(fecha,'YYYY-MM'), sum(monto)::bigint FROM cobro WHERE tenant=1 GROUP BY 1 ORDER BY 1")
webm = {r[0]: int(r[1]) for r in pc.fetchall()}
for ym in sorted(set(legm) | set(webm)):
    l = legm.get(ym, 0); w = webm.get(ym, 0); ok = (l == w); fails += (0 if ok else 1)
    print("  %-8s legado=%-14s web=%-14s %s" % (ym, format(l, ",d"), format(w, ",d"), "OK" if ok else "*** DIF ***"))

# cuadre por cuota: estado CANCELADO/PENDIENTE por (operacion,numero_cuota) contra el legado
print("== CUADRE POR CUOTA (estado exacto) ==")
m = json.load(open(SP + "/mig_map_activos.json")); map_prop = {int(k): v for k, v in m["prop"].items()}
fc.execute("SELECT SOCIO_NEGOCIO_ID, NUMERO_DOCUMENTO FROM SOCIOS_NEGOCIOS"); doc_by_socio = {sid: str(d).strip() for sid, d in fc.fetchall() if d}
pc.execute("SELECT numero_documento, persona FROM persona"); pers_by_doc = {r[0]: r[1] for r in pc.fetchall()}
fc.execute("SELECT OPERACION_PROPIEDAD_ID, SOCIO_NEGOCIO_ID, PROPIEDAD_ID FROM OPERACIONES_PROPIEDADES")
key_by_oid = {oid: (map_prop.get(pid), pers_by_doc.get(doc_by_socio.get(sid))) for oid, sid, pid in fc.fetchall()}
fc.execute("SELECT OPERACION_PROPIEDAD_ID, NUMERO_CUOTA, ESTADO FROM CRONOGRAMAS_CUOTAS")
leg = fc.fetchall()
mismatch = 0; comparadas = 0
for oid, ncuo, est in leg:
    act, cli = key_by_oid.get(oid, (None, None))
    if not act or not cli: continue
    pc.execute("SELECT cc.estado FROM cronograma_cuota cc JOIN operacion o ON o.operacion=cc.operacion "
               "WHERE o.tenant=1 AND o.activo=%s AND o.cliente=%s AND cc.numero_cuota=%s", (act, cli, int(ncuo)))
    r = pc.fetchone()
    if r is None: continue
    comparadas += 1
    if r[0] != str(est).strip().upper(): mismatch += 1
fails += mismatch
print("  cuotas comparadas: %d  mismatches de estado: %d %s" % (comparadas, mismatch, "OK" if mismatch == 0 else "*** REVISAR ***"))

fb.close(); pg.close()
print("\nRESULTADO:", "TODO CUADRA" if fails == 0 else ("%d discrepancias" % fails))
sys.exit(0 if fails == 0 else 1)
