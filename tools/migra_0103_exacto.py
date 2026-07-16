#!/usr/bin/env python3
"""
REQ-0103 Fase 9 (obs 320/321) - Cronograma con importes/fechas EXACTOS del legado.

Codex (obs 321): regenerar el cronograma con f_generar_cronograma altera los importes historicos
(1.225.995.000 web vs 1.224.081.000 legado). Esta herramienta reconstruye la capa cronograma+
documento+cobros del tenant 1 usando los valores REALES de CRONOGRAMAS_CUOTAS del legado
(numero_cuota, fecha_vencimiento, monto, estado, fecha_cancelacion), sin regenerar nada.

Flujo por operacion (respetando el motor para los cobros):
  0) borra la capa financiera del tenant 1 (cobro/detalle, cuotas, documento/detalle) - conserva operacion/activo.
  1) reidentifica la operacion nueva por (activo, cliente) contra el legado.
  2) fija operacion.monto_total_operacion = suma exacta de cuotas del legado.
  3) crea el documento interno DINT/OP (total = suma legado) y enlaza.
  4) inserta las cuotas del legado VERBATIM (monto, fecha, moneda) con saldo=monto.
  5) cobra por f_cobrar_documento la suma de las cuotas CANCELADAS del legado -> el motor baja los
     saldos y marca CANCELADO exactamente por ese monto (FIFO por numero de cuota).
  6) ajusta activo.estado (ALQUILER vigente -> OCUPADA, VENTA -> VENDIDA).

  python tools/migra_0103_exacto.py --dry-run
  python tools/migra_0103_exacto.py --apply
"""
import os, argparse, json

SP = r"C:/Users/everm/AppData/Local/Temp/claude/C--Users-everm-OneDrive-Documents-Datos-claude-semaforo-semaforo-adaptivo-desarrollo/a89655ce-0ba4-4159-82b6-6f74ec57e4a3/scratchpad"
FB = SP + "/fb/fb25"
os.environ["FIREBIRD"] = FB
os.environ["PATH"] = FB + os.pathsep + os.environ.get("PATH", "")
import fdb
fdb.load_api(FB + "/fbembed.dll")
import psycopg2

FDB = r"C:/Users/everm/OneDrive/Documents/Datos/Sistemas/2R/Desarrollo/SGInmo/codigo fuente/inmobiliaria/Pysistemas/migracion/source/INMOBILIARIA.FDB"
TENANT = 1; FORMA_EFECTIVO = 1

def env_pw():
    for l in open(os.path.join(os.path.dirname(__file__), "..", ".env"), encoding="utf-8"):
        if l.startswith("APP_DB_PASS="):
            return l.split("=", 1)[1].strip()

def crear_documento_interno(pc, tenant, fecha, persona, sucursal, moneda, monto, concepto):
    pc.execute("UPDATE rango_comprobante SET estado='INACTIVO', usuario_modificacion='migracion', fecha_modificacion=now() "
               "WHERE tenant=%s AND tipo='DINT' AND serie='OP' AND estado='ACTIVO' AND numero_actual > numero_hasta", (tenant,))
    pc.execute("UPDATE rango_comprobante SET estado='ACTIVO', usuario_modificacion='migracion', fecha_modificacion=now() "
               "WHERE rango_comprobante = (SELECT rc.rango_comprobante FROM rango_comprobante rc "
               "  WHERE rc.tenant=%s AND rc.tipo='DINT' AND rc.serie='OP' AND rc.estado<>'ACTIVO' "
               "    AND rc.numero_actual <= rc.numero_hasta ORDER BY rc.numero_desde LIMIT 1) "
               "AND NOT EXISTS (SELECT 1 FROM rango_comprobante a WHERE a.tenant=%s AND a.tipo='DINT' "
               "  AND a.serie='OP' AND a.estado='ACTIVO' AND a.numero_actual <= a.numero_hasta)", (tenant, tenant))
    pc.execute("INSERT INTO rango_comprobante (tenant, tipo, serie, numero_desde, numero_actual, numero_hasta, estado, usuario_creacion, fecha_creacion) "
               "SELECT %s,'DINT','OP', b.n, b.n, b.n + 1000000000, 'ACTIVO','migracion',now() "
               "FROM (SELECT COALESCE(MAX(rc.numero_hasta),0)+1 AS n FROM rango_comprobante rc WHERE rc.tenant=%s AND rc.tipo='DINT' AND rc.serie='OP') b "
               "WHERE NOT EXISTS (SELECT 1 FROM rango_comprobante a WHERE a.tenant=%s AND a.tipo='DINT' AND a.serie='OP' AND a.estado='ACTIVO' AND a.numero_actual <= a.numero_hasta)",
               (tenant, tenant, tenant))
    pc.execute("SELECT f_siguiente_numero(%s,'DINT','OP')", (tenant,)); num = pc.fetchone()[0]
    pc.execute("INSERT INTO documento (tenant, empresa, tipo, serie, numero, fecha, persona, sucursal, moneda, direccion_dinero, observacion, usuario_creacion, fecha_creacion) "
               "VALUES (%s,%s,'DINT','OP',%s,%s,%s,%s,%s,'ENTRADA',%s,'migracion',now()) RETURNING documento",
               (tenant, tenant, num, fecha, persona, sucursal, moneda, concepto[:200]))
    doc = pc.fetchone()[0]
    pc.execute("INSERT INTO documento_detalle (documento, numero_item, concepto, cantidad, precio_unitario, monto, saldo, usuario_creacion, fecha_creacion) "
               "VALUES (%s,1,%s,1,%s,%s,%s,'migracion',now())", (doc, concepto[:200], monto, monto, monto))
    return doc

def main():
    ap = argparse.ArgumentParser(); ap.add_argument("--apply", action="store_true"); ap.add_argument("--dry-run", action="store_true")
    a = ap.parse_args(); apply = a.apply and not a.dry_run

    fb = fdb.connect(database=FDB, user="SYSDBA", password="masterkey", charset="WIN1252"); fc = fb.cursor()
    pg = psycopg2.connect(host="127.0.0.1", port=15432, dbname="sginmo", user="sginmo", password=env_pw(), connect_timeout=15)
    pc = pg.cursor(); pc.execute("SET app.tenant='-1'")

    # legado: cuotas por operacion + moneda mapeada
    pc.execute("SELECT moneda, simbolo FROM moneda WHERE tenant IN (-1,1)"); new_mon = {r[1].strip().upper(): r[0] for r in pc.fetchall()}
    fc.execute("SELECT MONEDA_ID, SIMBOLO FROM MONEDAS"); MON = {mid: new_mon.get(str(s or "").strip().upper(), 1) for mid, s in fc.fetchall()}
    fc.execute("SELECT OPERACION_PROPIEDAD_ID, NUMERO_CUOTA, FECHA_VENCIMIENTO, MONTO, ESTADO, FECHA_CANCELACION, MONEDA_ID FROM CRONOGRAMAS_CUOTAS ORDER BY OPERACION_PROPIEDAD_ID, NUMERO_CUOTA")
    leg_cuotas = {}
    for oid, ncuo, fven, monto, est, fcanc, monid in fc.fetchall():
        leg_cuotas.setdefault(oid, []).append((int(ncuo), fven, monto, str(est or "").strip().upper(), fcanc, MON.get(monid, 1)))
    # mapa (activo,cliente) -> oid legado
    m = json.load(open(SP + "/mig_map_activos.json")); map_prop = {int(k): v for k, v in m["prop"].items()}
    fc.execute("SELECT SOCIO_NEGOCIO_ID, NUMERO_DOCUMENTO FROM SOCIOS_NEGOCIOS"); doc_by_socio = {sid: str(d).strip() for sid, d in fc.fetchall() if d}
    pc.execute("SELECT numero_documento, persona FROM persona"); pers_by_doc = {r[0]: r[1] for r in pc.fetchall()}
    fc.execute("SELECT OPERACION_PROPIEDAD_ID, SOCIO_NEGOCIO_ID, PROPIEDAD_ID FROM OPERACIONES_PROPIEDADES")
    oid_by_key = {}
    for oid, sid, pid in fc.fetchall():
        act = map_prop.get(pid); cli = pers_by_doc.get(doc_by_socio.get(sid))
        if act and cli: oid_by_key[(act, cli)] = oid

    if not apply:
        tot = sum(sum(x[2] for x in v) for v in leg_cuotas.values())
        print("== DRY-RUN exacto ==  operaciones legado:", len(leg_cuotas), " cuotas:", sum(len(v) for v in leg_cuotas.values()), " suma:", format(int(tot), ",d"))
        pc.close(); pg.close(); fb.close(); return

    # 0) limpiar capa financiera del tenant 1 (conserva operacion/activo)
    print("== 0) limpieza capa financiera tenant 1 ==")
    OPS = "(SELECT operacion FROM operacion WHERE tenant=1)"; DOC = "(SELECT documento FROM documento WHERE tenant=1)"; COB = "(SELECT cobro FROM cobro WHERE tenant=1)"
    for stmt in [
        "DELETE FROM anulacion WHERE cobro IN %s OR documento IN %s" % (COB, DOC),
        "DELETE FROM dato_cobro WHERE cobro IN %s" % COB,
        "DELETE FROM cobro_detalle WHERE cobro IN %s" % COB,
        "DELETE FROM cobro WHERE tenant=1",
        "DELETE FROM documento_generado WHERE cronograma_cuota IN (SELECT cronograma_cuota FROM cronograma_cuota WHERE operacion IN %s)" % OPS,
        "DELETE FROM cronograma_cuota WHERE operacion IN %s OR documento IN %s" % (OPS, DOC),
        "DELETE FROM documento_detalle WHERE documento IN %s" % DOC,
        "DELETE FROM documento WHERE tenant=1"]:
        pc.execute("SAVEPOINT s")
        try: pc.execute(stmt); print("  %-20s -> %d" % (stmt.split()[2], pc.rowcount)); pc.execute("RELEASE SAVEPOINT s")
        except psycopg2.Error as e: pc.execute("ROLLBACK TO SAVEPOINT s"); print("  omitida:", str(e).splitlines()[0][:50])

    # planilla de migracion
    pc.execute("INSERT INTO planilla (tenant, sucursal, estado, usuario_apertura, fecha_apertura, hora_apertura, usuario_creacion, fecha_creacion) "
               "VALUES (1,1,'ABIERTA','migracion',current_date,now(),'migracion',now()) RETURNING planilla")
    planilla = pc.fetchone()[0]

    pc.execute("SELECT o.operacion, o.activo, o.cliente, o.tipo_operacion, o.estado, o.fecha_operacion, o.sucursal, o.moneda, COALESCE(a.nombre,'') "
               "FROM operacion o LEFT JOIN activo a ON a.activo=o.activo WHERE o.tenant=1 ORDER BY o.operacion")
    ops = pc.fetchall()
    print("== 1-5) reconstruccion exacta por operacion ==")
    n_op = n_cuo = n_cob = 0; tot_cuo = tot_cob = 0
    for (op, activo, cli, tipo, est, fop, suc, mon, anom) in ops:
        oid = oid_by_key.get((activo, cli)); cuotas = leg_cuotas.get(oid, [])
        if not cuotas:
            continue
        suma = sum(c[2] for c in cuotas)
        pagado = sum(c[2] for c in cuotas if c[3] == "CANCELADO")
        pc.execute("UPDATE operacion SET monto_total_operacion=%s WHERE operacion=%s", (suma, op))
        concepto = ("Alquiler " if tipo == "ALQUILER" else "Venta ") + anom + " - Operacion " + str(op)
        doc = crear_documento_interno(pc, TENANT, fop, cli, suc or 1, mon, suma, concepto)
        for (ncuo, fven, monto, cest, fcanc, cmon) in cuotas:
            fv = fven.date() if hasattr(fven, "date") else fven
            pc.execute("INSERT INTO cronograma_cuota (operacion, numero_cuota, fecha_vencimiento, monto, saldo, moneda, documento, usuario_creacion, fecha_creacion) "
                       "VALUES (%s,%s,%s,%s,%s,%s,%s,'migracion',now())", (op, ncuo, fv, monto, monto, cmon, doc))
            n_cuo += 1
        n_op += 1; tot_cuo += int(suma)
        # un cobro POR CUOTA pagada, con su fecha real de cancelacion (o el vencimiento si el legado no la
        # registro) -> la evolucion mensual de cobros refleja cuando se pago de verdad (no amontonado).
        for (ncuo, fven, monto, cest, fcanc, cmon) in cuotas:
            if cest != "CANCELADO":
                continue
            fv = fven.date() if hasattr(fven, "date") else fven
            fpago = (fcanc.date() if hasattr(fcanc, "date") else fcanc) if fcanc else fv
            pc.execute("SELECT f_cobrar_documento(%s::bigint,%s::bigint,%s::bigint,%s::bigint,%s::numeric,%s::bigint,%s::date,%s::varchar,"
                       "NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL)",
                       (doc, planilla, FORMA_EFECTIVO, cli, monto, mon, fpago, "migracion"))
            n_cob += 1; tot_cob += int(monto)
        # 5b) obs 323: persistir el estado/saldo/fecha_cancelacion EXACTOS del legado por cuota.
        # f_actualiza_saldo_cuotas (que corre dentro de f_cobrar) resetea las cuotas por FIFO y pone
        # fecha_cancelacion=current_date; aca se sobrescribe cada cuota con su estado real historico.
        # El total pagado por documento no cambia (se cobro exactamente la suma de las canceladas), por lo
        # que sum(cuota.saldo) sigue == documento.saldo (cuotas de igual monto).
        for (ncuo, fven, monto, cest, fcanc, cmon) in cuotas:
            fv = fven.date() if hasattr(fven, "date") else fven
            if cest == "CANCELADO":
                fcanc_d = (fcanc.date() if hasattr(fcanc, "date") else fcanc) if fcanc else fv
                pc.execute("UPDATE cronograma_cuota SET estado='CANCELADO', saldo=0, fecha_cancelacion=%s, version=version+1 "
                           "WHERE operacion=%s AND numero_cuota=%s", (fcanc_d, op, ncuo))
            else:
                pc.execute("UPDATE cronograma_cuota SET estado='PENDIENTE', saldo=monto, fecha_cancelacion=NULL, version=version+1 "
                           "WHERE operacion=%s AND numero_cuota=%s", (op, ncuo))
        # 6) estado activo
        if activo:
            if tipo == "VENTA": pc.execute("UPDATE activo SET estado='VENDIDA' WHERE activo=%s", (activo,))
            elif tipo == "ALQUILER" and est == "VIGENTE": pc.execute("UPDATE activo SET estado='OCUPADA' WHERE activo=%s", (activo,))

    pc.execute("UPDATE planilla SET estado='CERRADA', usuario_modificacion='migracion', fecha_modificacion=now() WHERE planilla=%s", (planilla,))
    pg.commit()
    print("  operaciones:", n_op, " cuotas (exactas):", n_cuo, " suma cuotas:", format(tot_cuo, ",d"),
          " cobros:", n_cob, " total cobrado:", format(tot_cob, ",d"))
    pc.close(); pg.close(); fb.close()

if __name__ == "__main__":
    main()
