#!/usr/bin/env python3
"""
Control de cobertura de campos por ABM
======================================
Compara, para cada tabla con ABM, las columnas REALES de la base contra los
campos que la entidad JPA mapea (@Column / @JoinColumn). Reporta las columnas
de negocio que NO estan expuestas en el ABM, para que ningun campo quede olvidado
(el caso que motivo este control: Articulos tenia ~7 columnas sin exponer).

Se ignoran a proposito:
  - columnas de auditoria/sistema (usuario_creacion, fecha_creacion, ..., version)
  - la PK (se mapea como `id`)
  - un `_lista` cuyo `_codigo` par SI esta mapeado (la lista es un default fijo;
    el ABM solo elige el codigo)
  - columnas de negocio intencionalmente ocultas (derivadas / seteadas por SP /
    enlaces de integracion), declaradas en OCULTAS_OK y documentadas en
    docs-migracion/12-control-cobertura-abm.md

Origen de las columnas de BD (una de las dos):
  1) --csv <archivo>  : CSV `tabla,columna` (por ej. exportado con psql \copy)
  2) por defecto: lo trae de la VPS por SSH:
       ssh sginmo-vps "psql ... COPY(...) TO STDOUT CSV"
     usando la clave en el env var SGINMO_PG_PASS.

Uso:
  SGINMO_PG_PASS=... python tools/control_cobertura_abm.py
  python tools/control_cobertura_abm.py --csv columnas.csv
Salida: lista por ABM + total de huecos. Exit code != 0 si hay huecos.
"""
import re, glob, os, sys, subprocess

RAIZ = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
SRC  = os.path.join(RAIZ, "Desarrollo", "sginmo-web", "src", "main", "java")

SISTEMA = {"usuario_creacion", "fecha_creacion", "usuario_modificacion", "fecha_modificacion", "version"}

# columnas de negocio intencionalmente OCULTAS (ver doc 12); no son huecos del ABM
OCULTAS_OK = {
    "operacion.monto_total_letras",   # importe en letras; se calcula al imprimir, no se digita
    "cronograma_cuota.moneda",        # heredada de la operacion; la setea el SP generador
    "ingreso_egreso.documento",       # FK al comprobante; se enlaza por integracion, no en caja manual
}

# tabla -> entidades JPA que la mapean (PK compartida => varias entidades)
TABLAS = {
    "articulo": ["Articulo.java"], "moneda": ["Moneda.java"], "impuesto": ["Impuesto.java"],
    "forma_pago": ["FormaPago.java"], "entidad": ["Entidad.java"], "parametro_sistema": ["ParametroSistema.java"],
    "ubicacion_geografica": ["UbicacionGeografica.java"], "usuario": ["Usuario.java"], "grupo": ["Grupo.java"],
    "persona": ["Persona.java"], "persona_fisica": ["PersonaFisica.java"], "persona_juridica": ["PersonaJuridica.java"],
    "sucursal": ["Sucursal.java"], "activo": ["Activo.java"], "operacion": ["Operacion.java"],
    "cronograma_cuota": ["CronogramaCuota.java"], "planilla": ["Planilla.java"],
    "ingreso_egreso": ["IngresoEgreso.java"], "liquidacion": ["Liquidacion.java"],
}

def indexar_javas():
    idx = {}
    for base in (SRC, os.path.join(RAIZ, "Desarrollo", "onesystem-security", "src", "main", "java")):
        for p in glob.glob(os.path.join(base, "**", "*.java"), recursive=True):
            idx.setdefault(os.path.basename(p), p)
    return idx

def mapeadas(archivo, idx):
    p = idx.get(archivo)
    if not p:
        return None
    s = open(p, encoding="utf-8").read()
    return set(re.findall(r'@(?:Join)?Column\(name\s*=\s*"([^"]+)"', s))

def columnas_bd(args):
    if "--csv" in args:
        ruta = args[args.index("--csv") + 1]
        texto = open(ruta, encoding="utf-8").read()
    else:
        tablas = "','".join(TABLAS.keys())
        sql = ("COPY (SELECT table_name, column_name FROM information_schema.columns "
               "WHERE table_schema='public' AND table_name IN ('%s') "
               "ORDER BY table_name, ordinal_position) TO STDOUT WITH CSV" % tablas)
        passw = os.environ.get("SGINMO_PG_PASS", "")
        cmd = ["ssh", "-o", "BatchMode=yes", "sginmo-vps",
               "PGPASSWORD=%s psql -U sginmo -h localhost -d sginmo -tAc \"%s\"" % (passw, sql)]
        texto = subprocess.check_output(cmd, text=True)
    cols = {}
    for line in texto.splitlines():
        line = line.strip()
        if "," in line:
            t, c = line.split(",", 1)
            cols.setdefault(t, []).append(c)
    return cols

def main():
    args = sys.argv[1:]
    dbcols = columnas_bd(args)
    idx = indexar_javas()
    total = 0
    for tabla, archivos in TABLAS.items():
        cols = dbcols.get(tabla, [])
        mapped = set()
        for a in archivos:
            m = mapeadas(a, idx)
            if m:
                mapped |= m
        faltan = []
        for c in cols:
            if c in mapped or c in SISTEMA or c == tabla:
                continue
            if c.endswith("_lista") and (c[:-6] + "_codigo") in mapped:
                continue
            if f"{tabla}.{c}" in OCULTAS_OK:
                continue
            faltan.append(c)
        marca = "  " if not faltan else ">>"
        estado = "OK" if not faltan else f"FALTAN {len(faltan)} -> {', '.join(faltan)}"
        print(f"{marca} {tabla:22} {estado}")
        total += len(faltan)
    print(f"\n=== Columnas de negocio SIN exponer: {total} ===")
    return 1 if total else 0

if __name__ == "__main__":
    sys.exit(main())
