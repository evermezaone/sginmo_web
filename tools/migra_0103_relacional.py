#!/usr/bin/env python3
"""
REQ-0103 Fases 3-4 - Migracion relacional: activos + operaciones + cronogramas (motor).
Lee INMOBILIARIA.FDB (Firebird embebido) y escribe en PostgreSQL (VPS via tunel 127.0.0.1:15432),
construyendo mapas de ID legado->nuevo. Usa f_generar_cronograma para las cuotas.

  python tools/migra_0103_relacional.py --clean-activos   # limpia datos de prueba transaccionales del tenant 1
  python tools/migra_0103_relacional.py --activos          # migra activos (entidades+propiedades+propietarios)
  python tools/migra_0103_relacional.py --operaciones      # migra operaciones + cronogramas (requiere --activos previo)
  (se pueden combinar; --dry-run reporta sin escribir)
"""
import os, sys, argparse

SP = r"C:/Users/everm/AppData/Local/Temp/claude/C--Users-everm-OneDrive-Documents-Datos-claude-semaforo-semaforo-adaptivo-desarrollo/a89655ce-0ba4-4159-82b6-6f74ec57e4a3/scratchpad"
FB = SP + "/fb/fb25"
os.environ["FIREBIRD"] = FB
os.environ["PATH"] = FB + os.pathsep + os.environ.get("PATH", "")
import fdb
fdb.load_api(FB + "/fbembed.dll")
import psycopg2

FDB = r"C:/Users/everm/OneDrive/Documents/Datos/Sistemas/2R/Desarrollo/SGInmo/codigo fuente/inmobiliaria/Pysistemas/migracion/source/INMOBILIARIA.FDB"
TENANT = 1
SUCURSAL = 1
USR = "migracion"

# TIPOS_ACTIVO (entidad, -1)
TA = {"CASA":10,"DEPARTAMENTO":11,"DUPLEX":12,"LOTE":13,"OFICINA":14,"PIEZA":15,"SALONES":16,
      "ESTACIONAMIENTO":17,"AREA_COMUN":18,"EDIFICIO":5,"COMPLEJO":6,"LOTEAMIENTO":7,
      "BARRIO_CERRADO":8,"SALONES_COMERCIALES":9}
TC = {"PRIVADO":19,"PUBLICO":20}
TF = {"FINANCIACION_PROPIA":21,"FINANCIACION_BANCARIA":22,"PROPIA":21,"BANCARIA":22}

def env_pw():
    for l in open(os.path.join(os.path.dirname(__file__), "..", ".env"), encoding="utf-8"):
        if l.startswith("APP_DB_PASS="):
            return l.split("=",1)[1].strip()

def tipo_activo(s):
    s = str(s or "").strip().upper().replace(" ", "_")
    if s in TA: return TA[s]
    for k in TA:
        if k in s or s in k: return TA[k]
    return TA["LOTE"]   # default razonable

def norm_tipo_op(s):
    s = str(s or "").strip().upper()
    return "VENTA" if "VENT" in s or s == "V" else "ALQUILER"

def norm_estado_op(s):
    s = str(s or "").strip().upper()
    if "FIN" in s or "CANC" in s or "RESC" in s: return "FINALIZADO"
    return "VIGENTE"

def norm_cond(s):
    s = str(s or "").strip().upper()
    return "CONTADO" if "CONT" in s else "CREDITO"

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--clean-activos", action="store_true")
    ap.add_argument("--activos", action="store_true")
    ap.add_argument("--operaciones", action="store_true")
    ap.add_argument("--dry-run", action="store_true")
    a = ap.parse_args()

    fb = fdb.connect(database=FDB, user="SYSDBA", password="masterkey", charset="WIN1252")
    fc = fb.cursor()
    pg = psycopg2.connect(host="127.0.0.1", port=15432, dbname="sginmo", user="sginmo", password=env_pw(), connect_timeout=15)
    pc = pg.cursor()
    pc.execute("SET app.tenant = '-1'")

    # mapa moneda legado->nuevo por simbolo (fallback Gs=1)
    pc.execute("SELECT moneda, simbolo FROM moneda WHERE tenant IN (-1,1)")
    new_mon = {r[1].strip().upper(): r[0] for r in pc.fetchall()}
    fc.execute("SELECT MONEDA_ID, SIMBOLO FROM MONEDAS")
    MON = {}
    for mid, sim in fc.fetchall():
        MON[mid] = new_mon.get(str(sim or "").strip().upper(), 1)
    monof = lambda m: MON.get(m, 1)

    if a.clean_activos:
        print("== LIMPIEZA transaccional del tenant %d (data de prueba, orden de FK) ==" % TENANT)
        T = TENANT
        OPS = "(SELECT operacion FROM operacion WHERE tenant=%d)" % T
        COB = "(SELECT cobro FROM cobro WHERE tenant=%d)" % T
        DOC = "(SELECT documento FROM documento WHERE tenant=%d)" % T
        ACT = "(SELECT activo FROM activo WHERE tenant=%d)" % T
        CUO = "(SELECT cc.cronograma_cuota FROM cronograma_cuota cc JOIN operacion o ON o.operacion=cc.operacion WHERE o.tenant=%d)" % T
        deletes = [
            "DELETE FROM documento_generado WHERE operacion IN %s OR cronograma_cuota IN %s" % (OPS, CUO),
            "DELETE FROM gestion_cobranza WHERE operacion IN %s" % OPS,
            "DELETE FROM promesa_pago WHERE operacion IN %s" % OPS,
            "DELETE FROM rescision WHERE operacion IN %s" % OPS,
            "DELETE FROM anulacion WHERE cobro IN %s OR documento IN %s" % (COB, DOC),
            "DELETE FROM dato_cobro WHERE cobro IN %s" % COB,
            "DELETE FROM cobro_detalle WHERE cobro IN %s" % COB,
            "DELETE FROM portal_pago_qr WHERE tenant=%d" % T,
            "DELETE FROM agenda_evento WHERE tenant=%d" % T,
            "DELETE FROM archivo_adjunto WHERE tenant=%d" % T,
            "DELETE FROM liquidacion_detalle WHERE liquidacion IN (SELECT liquidacion FROM liquidacion WHERE operacion IN %s)" % OPS,
            "DELETE FROM liquidacion WHERE operacion IN %s" % OPS,
            "DELETE FROM cobro WHERE tenant=%d" % T,
            "DELETE FROM ingreso_egreso WHERE tenant=%d" % T,
            "DELETE FROM cronograma_cuota WHERE operacion IN %s OR documento IN %s" % (OPS, DOC),
            "DELETE FROM operacion WHERE tenant=%d" % T,
            "DELETE FROM documento_detalle WHERE documento IN %s" % DOC,
            "DELETE FROM documento WHERE tenant=%d" % T,
            "DELETE FROM activo_atributo WHERE activo IN %s" % ACT,
            "DELETE FROM activo_propietario WHERE activo IN %s" % ACT,
            "DELETE FROM activo WHERE tenant=%d" % T,
        ]
        for stmt in deletes:
            tabla = stmt.split()[2]
            if a.dry_run:
                print("  DRY:", stmt[:70]); continue
            pc.execute("SAVEPOINT sp")
            try:
                pc.execute(stmt); print("  %-22s -> %d" % (tabla, pc.rowcount)); pc.execute("RELEASE SAVEPOINT sp")
            except psycopg2.Error as e:
                pc.execute("ROLLBACK TO SAVEPOINT sp"); print("  %-22s -> OMITIDA (%s)" % (tabla, str(e).splitlines()[0][:50]))
        if not a.dry_run: pg.commit()

    map_ent = {}   # ENTIDAD_INMOBILIARIA_ID -> activo
    map_prop = {}  # PROPIEDAD_ID -> activo

    if a.activos:
        print("== ACTIVOS ==")
        # entidades inmobiliarias (padres)
        fc.execute("SELECT ENTIDAD_INMOBILIARIA_ID, NOMBRE, TIPO, DIRECCION, ESTADO, CUENTA_CATASTRAL, NUMERO_FINCA, OBSERVACION FROM ENTIDADES_INMOBILIARIAS")
        for r in fc.fetchall():
            eid, nom, tipo, dirc, est, cc, nf, obs = r
            if a.dry_run: map_ent[eid] = -1; continue
            pc.execute("INSERT INTO activo (nombre, tenant, tipo, direccion, estado, cuenta_catastral, numero_finca, observacion, usuario_creacion, fecha_creacion) "
                       "VALUES (%s,%s,%s,%s,'LIBRE',%s,%s,%s,%s,now()) RETURNING activo",
                       ((nom or "")[:120], TENANT, tipo_activo(tipo), (dirc or "")[:200], (cc or None), (nf or None), (obs or None), USR))
            map_ent[eid] = pc.fetchone()[0]
        # propiedades (hijos)
        fc.execute("SELECT PROPIEDAD_ID, NOMBRE, TIPO, ENTIDAD_INMOBILIARIA_ID, DIRECCION, ESTADO, CUENTA_CATASTRAL, NUMERO_FINCA, NUMERO_LOTE, NUMERO_MANZANA, PRECIO_VENTA, COMISION_VENTA, PRECIO_ALQUILER, COMISION_ALQUILER, OBSERVACION FROM PROPIEDADES")
        for r in fc.fetchall():
            pid, nom, tipo, eid, dirc, est, cc, nf, nl, nm, pv, cv, pa, ca, obs = r
            padre = map_ent.get(eid)
            if a.dry_run: map_prop[pid] = -1; continue
            pc.execute("INSERT INTO activo (nombre, tenant, padre, tipo, direccion, estado, cuenta_catastral, numero_finca, numero_lote, numero_manzana, precio_venta, comision_venta, precio_alquiler, comision_alquiler, observacion, usuario_creacion, fecha_creacion) "
                       "VALUES (%s,%s,%s,%s,%s,'LIBRE',%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,now()) RETURNING activo",
                       ((nom or "")[:120], TENANT, padre, tipo_activo(tipo), (dirc or "")[:200], (cc or None), (nf or None), (nl or None), (nm or None),
                        pv, cv, pa, ca, (obs or None), USR))
            map_prop[pid] = pc.fetchone()[0]
        # propietarios: PROPIETARIO_ID (socio) -> persona por documento
        fc.execute("SELECT NUMERO_DOCUMENTO, SOCIO_NEGOCIO_ID FROM SOCIOS_NEGOCIOS")
        doc_by_socio = {sid: str(doc).strip() for doc, sid in fc.fetchall() if doc}
        pc.execute("SELECT numero_documento, persona FROM persona")
        pers_by_doc = {r[0]: r[1] for r in pc.fetchall()}
        fc.execute("SELECT PROPIEDAD_ID, PROPIETARIO_ID FROM PROPIETARIOS_ENT_INMOB")
        np = 0
        for pid, prop_sid in fc.fetchall():
            act = map_prop.get(pid); doc = doc_by_socio.get(prop_sid); per = pers_by_doc.get(doc)
            if not act or not per: continue
            if a.dry_run: np += 1; continue
            pc.execute("INSERT INTO activo_propietario (activo, propietario, estado, usuario_creacion, fecha_creacion) "
                       "SELECT %s,%s,'ACTIVO',%s,now() WHERE NOT EXISTS (SELECT 1 FROM activo_propietario WHERE activo=%s AND propietario=%s)",
                       (act, per, USR, act, per))
            np += pc.rowcount
        if not a.dry_run: pg.commit()
        print("  entidades:", len(map_ent), " propiedades:", len(map_prop), " propietarios:", np)
        # persistir mapas para la fase de operaciones
        import json
        json.dump({"ent": {str(k): v for k, v in map_ent.items()}, "prop": {str(k): v for k, v in map_prop.items()}},
                  open(SP + "/mig_map_activos.json", "w"))

    if a.operaciones:
        import json
        m = json.load(open(SP + "/mig_map_activos.json"))
        map_prop = {int(k): v for k, v in m["prop"].items()}
        # socio->documento->persona
        fc.execute("SELECT SOCIO_NEGOCIO_ID, NUMERO_DOCUMENTO FROM SOCIOS_NEGOCIOS")
        doc_by_socio = {sid: str(doc).strip() for sid, doc in fc.fetchall() if doc}
        pc.execute("SELECT numero_documento, persona FROM persona")
        pers_by_doc = {r[0]: r[1] for r in pc.fetchall()}
        print("== OPERACIONES + CRONOGRAMAS ==")
        fc.execute("""SELECT OPERACION_PROPIEDAD_ID, FECHA_OPERACION, TIPO_OPERACION, SOCIO_NEGOCIO_ID, PROPIEDAD_ID,
                             FECHA_INICIO_CONTRATO, FECHA_FIN_CONTRATO, FECHA_FINALIZACION, PLAZO, PRECIO, MONTO_TOTAL_OPERACION,
                             GARANTIA, ESTADO, MONEDA_ID, CONDICION_OPERACION, DIA_PAGO, MONTO_MORA, DIAS_GRACIA,
                             TIPO_CONTRATO, TIPO_FINANCIACION, VENDEDOR_ID FROM OPERACIONES_PROPIEDADES""")
        rows = fc.fetchall()
        no = ncron = skip = 0
        # cantidad de cuotas por operacion (del legado)
        fc.execute("SELECT OPERACION_PROPIEDAD_ID, COUNT(*), MIN(FECHA_VENCIMIENTO), SUM(MONTO) FROM CRONOGRAMAS_CUOTAS GROUP BY OPERACION_PROPIEDAD_ID")
        cuotas_info = {r[0]: (r[1], r[2], r[3]) for r in fc.fetchall()}
        for r in rows:
            (oid, fop, tipo_op, sid, pid, fic, ffc, ffin, plazo, precio, mtot, gar, est, monid, cond, diap, mora, diasg, tcon, tfin, vend) = r
            act = map_prop.get(pid); doc = doc_by_socio.get(sid); cli = pers_by_doc.get(doc)
            if not act or not cli:
                skip += 1; continue
            mon = monof(monid)
            cant, fprim, sumc = cuotas_info.get(oid, (0, None, None))
            montotal = mtot or sumc or precio or 0
            dia = int(diap) if diap else (fprim.day if fprim else 1)
            tcont = TC.get(str(tcon or "").strip().upper())
            tfinan = TF.get(str(tfin or "").strip().upper())
            if a.dry_run: no += 1; ncron += (cant or 0); continue
            pc.execute("""INSERT INTO operacion (fecha_operacion, tipo_operacion, cliente, activo, fecha_inicio_contrato, fecha_fin_contrato,
                          fecha_finalizacion, plazo, precio, monto_total_operacion, garantia, estado, tenant, sucursal, moneda,
                          condicion_operacion, dia_pago, monto_mora, dias_gracia, tipo_contrato, tipo_financiacion, usuario_creacion, fecha_creacion)
                          VALUES (%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,now()) RETURNING operacion""",
                       (fop, norm_tipo_op(tipo_op), cli, act, fic, ffc, ffin, plazo, precio, montotal, gar or 0,
                        norm_estado_op(est), TENANT, SUCURSAL, mon, norm_cond(cond), (int(dia) if dia else 1), mora or 0,
                        int(diasg) if diasg else 0, tcont, tfinan, USR))
            new_op = pc.fetchone()[0]
            no += 1
            if cant and cant > 0 and montotal and float(montotal) > 0 and fprim:
                pc.execute("SELECT f_generar_cronograma(%s::bigint,%s::integer,%s::numeric,%s::date,%s::integer,%s::bigint,%s::varchar)",
                           (new_op, int(cant), float(montotal), fprim, int(dia) if dia else 1, mon, USR))
                ncron += 1
        if not a.dry_run: pg.commit()
        print("  operaciones:", no, " (omitidas por falta de activo/cliente:", skip, ")  cronogramas generados:", ncron)

    pc.close(); pg.close(); fb.close()

if __name__ == "__main__":
    main()
