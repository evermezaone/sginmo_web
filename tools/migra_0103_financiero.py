#!/usr/bin/env python3
"""
REQ-0103 Fase 8 (correccion) - Capa financiera coherente: documento interno + cobros por el motor + ocupacion.

Mi migracion inicial genero las cuotas con f_generar_cronograma pero SIN el documento interno DINT/OP
que respalda la cuenta corriente (paso que hace OperacionService.crearDocumentoInterno), y marco cuotas
'CANCELADO' tocando solo el estado y no el saldo -> estado incoherente (saldo por cobrar = total, cobros=0).

Esta herramienta, para el tenant 1, replicando el flujo real del sistema:
  1) resetea las cuotas a PENDIENTE/saldo=monto (deshace el marcado incoherente).
  2) por cada operacion con cronograma: crea el documento interno DINT/OP y enlaza las cuotas (documento=doc).
  3) crea una planilla de migracion (ABIERTA) y cobra por f_cobrar_documento el monto pagado del legado
     (equivalente a las primeras K cuotas, K = cuotas CANCELADAS del legado en esa operacion) -> el motor
     baja el saldo, marca cuotas CANCELADO y registra cobro+detalle (recaudacion visible en el dashboard).
  4) ajusta activo.estado: ALQUILER vigente -> OCUPADA, VENTA -> VENDIDA (como OperacionService).
  5) cierra la planilla de migracion.

  python tools/migra_0103_financiero.py --dry-run
  python tools/migra_0103_financiero.py --apply
"""
import os, sys, argparse, json

SP = r"C:/Users/everm/AppData/Local/Temp/claude/C--Users-everm-OneDrive-Documents-Datos-claude-semaforo-semaforo-adaptivo-desarrollo/a89655ce-0ba4-4159-82b6-6f74ec57e4a3/scratchpad"
FB = SP + "/fb/fb25"
os.environ["FIREBIRD"] = FB
os.environ["PATH"] = FB + os.pathsep + os.environ.get("PATH", "")
import fdb
fdb.load_api(FB + "/fbembed.dll")
import psycopg2

FDB = r"C:/Users/everm/OneDrive/Documents/Datos/Sistemas/2R/Desarrollo/SGInmo/codigo fuente/inmobiliaria/Pysistemas/migracion/source/INMOBILIARIA.FDB"
TENANT = 1
FORMA_EFECTIVO = 1

def env_pw():
    for l in open(os.path.join(os.path.dirname(__file__), "..", ".env"), encoding="utf-8"):
        if l.startswith("APP_DB_PASS="):
            return l.split("=", 1)[1].strip()

# --- documento interno DINT/OP (replica de OperacionService.crearDocumentoInterno) ---
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
    pc.execute("SELECT f_siguiente_numero(%s,'DINT','OP')", (tenant,))
    num = pc.fetchone()[0]
    pc.execute("INSERT INTO documento (tenant, empresa, tipo, serie, numero, fecha, persona, sucursal, moneda, direccion_dinero, observacion, usuario_creacion, fecha_creacion) "
               "VALUES (%s,%s,'DINT','OP',%s,%s,%s,%s,%s,'ENTRADA',%s,'migracion',now()) RETURNING documento",
               (tenant, tenant, num, fecha, persona, sucursal, moneda, concepto[:200]))
    doc = pc.fetchone()[0]
    pc.execute("INSERT INTO documento_detalle (documento, numero_item, concepto, cantidad, precio_unitario, monto, saldo, usuario_creacion, fecha_creacion) "
               "VALUES (%s,1,%s,1,%s,%s,%s,'migracion',now())", (doc, concepto[:200], monto, monto, monto))
    return doc

def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--apply", action="store_true")
    ap.add_argument("--dry-run", action="store_true")
    a = ap.parse_args()
    apply = a.apply and not a.dry_run

    fb = fdb.connect(database=FDB, user="SYSDBA", password="masterkey", charset="WIN1252"); fc = fb.cursor()
    pg = psycopg2.connect(host="127.0.0.1", port=15432, dbname="sginmo", user="sginmo", password=env_pw(), connect_timeout=15)
    pc = pg.cursor(); pc.execute("SET app.tenant='-1'")

    # --- legado: por operacion, cuotas pagadas (K) y fecha de la ultima cancelacion ---
    fc.execute("SELECT OPERACION_PROPIEDAD_ID, COUNT(*), MAX(FECHA_CANCELACION) FROM CRONOGRAMAS_CUOTAS WHERE ESTADO='CANCELADO' GROUP BY OPERACION_PROPIEDAD_ID")
    leg_pagos = {r[0]: (r[1], r[2]) for r in fc.fetchall()}
    fc.execute("SELECT OPERACION_PROPIEDAD_ID, SOCIO_NEGOCIO_ID, PROPIEDAD_ID FROM OPERACIONES_PROPIEDADES")
    leg_ops = fc.fetchall()

    # mapas activo/persona para reidentificar la operacion nueva por (activo, cliente)
    m = json.load(open(SP + "/mig_map_activos.json")); map_prop = {int(k): v for k, v in m["prop"].items()}
    fc.execute("SELECT SOCIO_NEGOCIO_ID, NUMERO_DOCUMENTO FROM SOCIOS_NEGOCIOS")
    doc_by_socio = {sid: str(doc).strip() for sid, doc in fc.fetchall() if doc}
    pc.execute("SELECT numero_documento, persona FROM persona"); pers_by_doc = {r[0]: r[1] for r in pc.fetchall()}

    # (activo,cli) -> (K cuotas pagadas, fecha) del legado
    pago_por_key = {}
    for oid, sid, pid in leg_ops:
        act = map_prop.get(pid); cli = pers_by_doc.get(doc_by_socio.get(sid))
        if act and cli and oid in leg_pagos:
            pago_por_key[(act, cli)] = leg_pagos[oid]

    # 1) reset de cuotas incoherentes (mi marcado previo)
    print("== 1) reset cuotas del tenant %d ==" % TENANT)
    if apply:
        pc.execute("UPDATE cronograma_cuota cc SET estado='PENDIENTE', saldo=cc.monto, fecha_cancelacion=NULL, version=cc.version+1 "
                   "FROM operacion o WHERE o.operacion=cc.operacion AND o.tenant=%s AND cc.estado='CANCELADO'", (TENANT,))
        print("   cuotas reseteadas:", pc.rowcount)

    # operaciones destino con cronograma
    pc.execute("SELECT o.operacion, o.activo, o.cliente, o.tipo_operacion, o.estado, o.fecha_operacion, o.sucursal, o.moneda, "
               "       COALESCE(a.nombre,''), (SELECT count(*) FROM cronograma_cuota c WHERE c.operacion=o.operacion) "
               "FROM operacion o LEFT JOIN activo a ON a.activo=o.activo WHERE o.tenant=%s ORDER BY o.operacion", (TENANT,))
    ops = pc.fetchall()

    # 2) planilla de migracion (ABIERTA)
    planilla = None
    if apply:
        pc.execute("INSERT INTO planilla (tenant, sucursal, estado, usuario_apertura, fecha_apertura, hora_apertura, usuario_creacion, fecha_creacion) "
                   "VALUES (%s,1,'ABIERTA','migracion',current_date,now(),'migracion',now()) RETURNING planilla", (TENANT,))
        planilla = pc.fetchone()[0]
        print("== 2) planilla de migracion:", planilla)

    print("== 3) documentos + cobros por operacion ==")
    n_doc = n_cob = 0; total_cobrado = 0
    for (op, activo, cli, tipo, est, fop, suc, mon, anom, ncuo) in ops:
        if ncuo and ncuo > 0:
            concepto = ("Alquiler " if tipo == "ALQUILER" else "Venta ") + anom + " - Operacion " + str(op)
            if apply:
                pc.execute("SELECT monto_total_operacion FROM operacion WHERE operacion=%s", (op,))
                monto_total = pc.fetchone()[0]
                doc = crear_documento_interno(pc, TENANT, fop, cli, suc or 1, mon, monto_total, concepto)
                pc.execute("UPDATE cronograma_cuota SET documento=%s WHERE operacion=%s", (doc, op)); n_doc += 1
                # monto pagado = suma de las primeras K cuotas (K = pagadas en el legado)
                kf = pago_por_key.get((activo, cli))
                if kf and kf[0] > 0:
                    K, fpago = kf
                    pc.execute("SELECT COALESCE(SUM(monto),0) FROM cronograma_cuota WHERE operacion=%s AND numero_cuota<=%s", (op, int(K)))
                    pagado = pc.fetchone()[0]
                    if pagado and pagado > 0:
                        fpago = fpago or fop
                        pc.execute("SELECT f_cobrar_documento(%s::bigint,%s::bigint,%s::bigint,%s::bigint,%s::numeric,%s::bigint,%s::date,%s::varchar,"
                                   "NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL,NULL)",
                                   (doc, planilla, FORMA_EFECTIVO, cli, pagado, mon, fpago, "migracion"))
                        n_cob += 1; total_cobrado += int(pagado)
        # 4) estado del activo (todas las operaciones, no solo credito)
        if apply and activo:
            if tipo == "VENTA":
                pc.execute("UPDATE activo SET estado='VENDIDA' WHERE activo=%s", (activo,))
            elif tipo == "ALQUILER" and est == "VIGENTE":
                pc.execute("UPDATE activo SET estado='OCUPADA' WHERE activo=%s", (activo,))

    # 5) cerrar planilla de migracion
    if apply:
        pc.execute("UPDATE planilla SET estado='CERRADA', usuario_modificacion='migracion', fecha_modificacion=now() WHERE planilla=%s", (planilla,))
        pg.commit()
    print("   documentos creados:", n_doc, " cobros:", n_cob, " total cobrado (Gs):", format(total_cobrado, ",d"))
    print("== 4) ocupacion ajustada (OCUPADA/VENDIDA segun operaciones) ==")
    if not apply:
        print("   (DRY-RUN: nada aplicado; operaciones con cronograma:", sum(1 for o in ops if o[9] and o[9] > 0), ")")

    pc.close(); pg.close(); fb.close()

if __name__ == "__main__":
    main()
