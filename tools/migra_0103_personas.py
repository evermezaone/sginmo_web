#!/usr/bin/env python3
"""
REQ-0103 Fase 2 - Migracion de SOCIOS_NEGOCIOS (Firebird legado) -> persona (+ fisica/juridica) + persona_rol.

Genera SQL idempotente (clave natural numero_documento) para el tenant destino. Dry-run por defecto:
  python tools/migra_0103_personas.py            # dry-run: reporta y escribe el .sql, NO aplica
  python tools/migra_0103_personas.py --emit-sql # solo escribe scratchpad/mig_personas.sql
El apply se hace aparte, canalizando el .sql a la VPS con psql (app.tenant=-1), tras revisar el dry-run.

Roles (entidad ROLES_PERSONA, global -1): CLIENTE=27, PROVEEDOR=28, PROPIETARIO=31, INQUILINO=32,
VENDEDOR=33, PORTERO=34, ADMINISTRADOR=35.
"""
import os, sys

SP = r"C:/Users/everm/AppData/Local/Temp/claude/C--Users-everm-OneDrive-Documents-Datos-claude-semaforo-semaforo-adaptivo-desarrollo/a89655ce-0ba4-4159-82b6-6f74ec57e4a3/scratchpad"
FB = SP + "/fb/fb25"
os.environ["FIREBIRD"] = FB
os.environ["PATH"] = FB + os.pathsep + os.environ.get("PATH", "")
import fdb
fdb.load_api(FB + "/fbembed.dll")
FDB = r"C:/Users/everm/OneDrive/Documents/Datos/Sistemas/2R/Desarrollo/SGInmo/codigo fuente/inmobiliaria/Pysistemas/migracion/source/INMOBILIARIA.FDB"

TENANT = 1
ROL = {"ES_CLIENTE": 27, "ES_PROVEEDOR": 28, "ES_PROPIETARIO": 31, "ES_INQUILINO": 32,
       "ES_VENDEDOR": 33, "ES_PORTERO": 34, "ES_ADMINISTRADOR": 35}

def q(s):
    if s is None: return "NULL"
    return "'" + str(s).replace("'", "''") + "'"

def esS(v):
    return str(v).strip().upper() in ("S", "SI", "TRUE", "1", "T", "Y")

con = fdb.connect(database=FDB, user="SYSDBA", password="masterkey", charset="WIN1252")
cur = con.cursor()
cur.execute("""SELECT SOCIO_NEGOCIO_ID, RAZON_SOCIAL, NUMERO_DOCUMENTO, DIGITO_VERIFICADOR, TIPO_PERSONERIA,
                      ACTIVO, DIRECCION, TELEFONO, EMAIL, SEXO,
                      ES_CLIENTE, ES_PROVEEDOR, ES_PROPIETARIO, ES_INQUILINO, ES_VENDEDOR, ES_PORTERO, ES_ADMINISTRADOR
               FROM SOCIOS_NEGOCIOS ORDER BY SOCIO_NEGOCIO_ID""")
filas = cur.fetchall()
cols = [d[0] for d in cur.description]
con.close()

sql = ["-- REQ-0103 Fase 2: personas + roles (idempotente por numero_documento). Tenant %d." % TENANT,
       "SELECT set_config('app.tenant','-1',false);", "BEGIN;"]
n_p = n_f = n_j = 0
roles_cnt = {}
sin_doc = 0
for r in filas:
    d = dict(zip(cols, r))
    doc = (str(d["NUMERO_DOCUMENTO"]).strip() if d["NUMERO_DOCUMENTO"] else "")
    if not doc:
        sin_doc += 1
        continue
    nombre = (d["RAZON_SOCIAL"] or "").strip()
    juridica = "JUR" in str(d["TIPO_PERSONERIA"] or "").strip().upper()   # PERJUR = juridica, PERFIS = fisica
    tipo_pers = "JURIDICA" if juridica else "FISICA"
    estado = "ACTIVO" if esS(d["ACTIVO"]) or d["ACTIVO"] is None else "INACTIVO"
    dv = (str(d["DIGITO_VERIFICADOR"]).strip()[:1] if d["DIGITO_VERIFICADOR"] else None)
    n_p += 1
    # persona (global): idempotente por numero_documento
    sql.append(
        "INSERT INTO persona (tipo_personeria, nombre, numero_documento, digito_verificador, estado, usuario_creacion, fecha_creacion) "
        "SELECT %s,%s,%s,%s,%s,'migracion',now() WHERE NOT EXISTS (SELECT 1 FROM persona WHERE numero_documento=%s);"
        % (q(tipo_pers), q(nombre[:120]), q(doc[:20]), q(dv), q(estado), q(doc[:20])))
    if juridica:
        n_j += 1
        sql.append(
            "INSERT INTO persona_juridica (persona, razon_social, usuario_creacion, fecha_creacion) "
            "SELECT p.persona,%s,'migracion',now() FROM persona p WHERE p.numero_documento=%s "
            "AND NOT EXISTS (SELECT 1 FROM persona_juridica j WHERE j.persona=p.persona);"
            % (q(nombre[:120]), q(doc[:20])))
    else:
        n_f += 1
        sx = str(d["SEXO"] or "").strip().upper()[:1]
        sexo = "MASCULINO" if sx == "M" else ("FEMENINO" if sx == "F" else None)
        sql.append(
            "INSERT INTO persona_fisica (persona, nombres, apellidos, sexo, usuario_creacion, fecha_creacion) "
            "SELECT p.persona,%s,'',%s,'migracion',now() FROM persona p WHERE p.numero_documento=%s "
            "AND NOT EXISTS (SELECT 1 FROM persona_fisica f WHERE f.persona=p.persona);"
            % (q(nombre[:120]), q(sexo), q(doc[:20])))
    # roles
    for flag, rol in ROL.items():
        if esS(d[flag]):
            roles_cnt[flag] = roles_cnt.get(flag, 0) + 1
            sql.append(
                "INSERT INTO persona_rol (persona, rol, tenant, estado, usuario_creacion, fecha_creacion) "
                "SELECT p.persona,%d,%d,'ACTIVO','migracion',now() FROM persona p WHERE p.numero_documento=%s "
                "AND NOT EXISTS (SELECT 1 FROM persona_rol pr WHERE pr.persona=p.persona AND pr.rol=%d AND pr.tenant=%d);"
                % (rol, TENANT, q(doc[:20]), rol, TENANT))
sql.append("COMMIT;")

out = SP + "/mig_personas.sql"
open(out, "w", encoding="utf-8").write("\n".join(sql) + "\n")

print("=== DRY-RUN Fase 2 (personas) ===")
print("socios leidos:", len(filas), " | con documento:", n_p, " | sin documento (omitidos):", sin_doc)
print("  -> persona_fisica:", n_f, " persona_juridica:", n_j)
print("  roles a asignar:", {k: v for k, v in sorted(roles_cnt.items())})
print("SQL generado:", out, "(", len(sql), "sentencias )")
print("Ejemplo (primeras filas de socios):")
for r in filas[:5]:
    d = dict(zip(cols, r))
    print("   doc=%s  %s  [%s]" % (d["NUMERO_DOCUMENTO"], (d["RAZON_SOCIAL"] or "")[:40], d["TIPO_PERSONERIA"]))
