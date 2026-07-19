# -*- coding: utf-8 -*-
"""
REQ-0107 - Cubo Excel conectado por ODBC al legado INMOBILIARIA.FDB.

Construye un libro con:
  - 5 hojas de DATOS, cada una es una consulta ODBC (QueryTable) contra el FDB -> refrescable con
    "Datos > Actualizar todo". Equivalen a las vistas v_datos_* de Metabase, sobre el esquema legado.
  - Tablas dinamicas y graficos por nivel (Direccion / Cobranza-Mora / Ocupacion / Recaudacion / Rentabilidad).
  - Hoja "Comparar" con los totales de control para cotejar contra Metabase.

Requiere: DSN ODBC 'SGInmo_FDB' (Firebird 2.0.5 + cliente 2.5), Excel 32-bit, pywin32.
Ejecutar:  python bi/construir_cubo_excel.py
El SQL de cada hoja viene de scratchpad/queries_legado.py (validado con el driver fdb).
"""
import os
import win32com.client as win32
import pythoncom

# El SQL complejo vive en 5 VISTAS del FDB (crear_vistas_legado.py). Excel solo hace SELECT * FROM vista,
# asi el driver ODBC 2.0.5 nunca ve expresiones complejas (las evalua Firebird nativo).
CONN = "ODBC;DSN=SGInmo_FDB;UID=SYSDBA;PWD=masterkey;"
# Carpeta LOCAL (fuera de OneDrive): el refresco programado escribe el archivo, y OneDrive bloquearia
# el handle al sincronizar -> locks y "copias en conflicto". Local evita eso por completo.
OUT = r"C:\Users\everm\SGInmoBI\SGInmo_Cubo_Legado.xlsx"

# constantes Excel
xlCmdSql = 2
xlDatabase = 1
xlRowField = 1
xlColumnField = 2
xlDataField = 4
xlSum = -4157
xlCount = -4112
xlColumnClustered = 51
xlPie = 5
xlLine = 4
xlColumnStacked = 52

DATOS = [
    ("Datos_Cuotas", "SELECT * FROM V_DATOS_CUOTA"),
    ("Datos_Cobros", "SELECT * FROM V_DATOS_COBRO"),
    ("Datos_Activos", "SELECT * FROM V_DATOS_ACTIVO"),
    ("Datos_Movimientos", "SELECT * FROM V_DATOS_MOVIMIENTO"),
    ("Datos_Contratos", "SELECT * FROM V_DATOS_CONTRATO"),
]


def main():
    pythoncom.CoInitialize()
    xl = win32.DispatchEx("Excel.Application")
    xl.Visible = False
    xl.DisplayAlerts = False
    xl.ScreenUpdating = False
    try:
        wb = xl.Workbooks.Add()
        # dejar una sola hoja base
        while wb.Worksheets.Count > 1:
            wb.Worksheets(wb.Worksheets.Count).Delete()

        # ---- 1) hojas de datos por ODBC ----
        rangos = {}
        for i, (nombre, sql) in enumerate(DATOS):
            ws = wb.Worksheets(1) if i == 0 else wb.Worksheets.Add(After=wb.Worksheets(wb.Worksheets.Count))
            ws.Name = nombre
            qt = ws.QueryTables.Add(Connection=CONN, Destination=ws.Range("A1"))
            qt.CommandType = xlCmdSql
            qt.CommandText = " ".join(sql.split())  # una sola linea: el driver 2.0.5 falla con saltos de linea
            qt.BackgroundQuery = False
            qt.RefreshOnFileOpen = True            # al abrir el libro, re-lee el FDB por ODBC
            qt.Name = "ext_" + nombre
            qt.Refresh(False)
            used = ws.UsedRange
            nr, nc = used.Rows.Count, used.Columns.Count
            rangos[nombre] = (ws, nr, nc)
            # formato de miles (sin decimales, separador segun locale = punto en es-PY) a columnas de monto
            for c in range(1, nc + 1):
                hdr = str(ws.Cells(1, c).Value or "").upper()
                if any(k in hdr for k in ("MONTO", "SALDO", "PRECIO", "GARANTIA")):
                    ws.Range(ws.Cells(2, c), ws.Cells(nr, c)).NumberFormat = "#,##0"
            ws.Rows(1).Font.Bold = True
            print("  hoja %-20s %d filas x %d cols" % (nombre, nr - 1, nc))

        def pivot(dest_ws, src_sheet, name, rows, cols, data, func=xlSum, chart=None, at="A1", chart_left=340, chart_top=8):
            ws, nr, nc = rangos[src_sheet]
            src_range = ws.Range(ws.Cells(1, 1), ws.Cells(nr, nc))
            pc = wb.PivotCaches().Create(SourceType=xlDatabase, SourceData=src_range)
            pt = pc.CreatePivotTable(TableDestination=dest_ws.Range(at), TableName=name)
            for f in rows:
                pt.PivotFields(f).Orientation = xlRowField
            for f in cols:
                pt.PivotFields(f).Orientation = xlColumnField
            try: pt.PivotCache().RefreshOnFileOpen = True   # el pivot tambien se refresca al abrir
            except Exception: pass
            df = pt.AddDataField(pt.PivotFields(data), "  " + data, func)
            try: df.NumberFormat = "#,##0"
            except Exception: pass
            if chart is not None:
                try:
                    sh = dest_ws.Shapes.AddChart2(-1, chart, chart_left, chart_top, 430, 230)
                    ch = sh.Chart
                    ch.SetSourceData(pt.TableRange1)
                    ch.ChartType = chart
                    ch.HasTitle = True
                    ch.ChartTitle.Text = name
                    try: ch.HasLegend = (chart in (xlPie, xlColumnStacked))
                    except Exception: pass
                except Exception as e:
                    m = ""
                    try: m = e.excepinfo[2]
                    except Exception: m = str(e)[:60]
                    print("     (chart %s omitido: %s)" % (name, m))
            return pt

        # ---- 2) tableros con pivots ----
        print("  == tableros ==")
        def P(*a, **k):
            try:
                pivot(*a, **k); print("     pivot OK:", a[2])
            except Exception as e:
                m = ""
                try: m = e.excepinfo[2]
                except Exception: m = str(e)[:70]
                print("     pivot FALLO %s: %s" % (a[2], m))

        # una tabla dinamica por hoja (Excel no permite 2 PivotCharts limpios en la misma hoja) + su grafico.
        TABLEROS = [
            ("1 Aging cartera", "Datos_Cuotas", "pt_aging", ["TRAMO_AGING"], [], "SALDO", xlSum, xlColumnClustered),
            ("2 Mora por tipo", "Datos_Cuotas", "pt_mora_tipo", ["TIPO_ACTIVO"], [], "SALDO", xlSum, xlColumnClustered),
            ("3 Situacion parque", "Datos_Activos", "pt_situacion", ["SITUACION"], [], "ACTIVO_ID", xlCount, xlPie),
            ("4 Ocupacion x tipo", "Datos_Activos", "pt_ocup_tipo", ["TIPO_ACTIVO"], ["SITUACION"], "ACTIVO_ID", xlCount, xlColumnStacked),
            ("5 Recaudacion x mes", "Datos_Cobros", "pt_cobro_mes", ["ANIO", "MES"], [], "MONTO", xlSum, xlColumnClustered),
            ("6 Recaudacion x tipo", "Datos_Cobros", "pt_cobro_tipo", ["TIPO_OPERACION"], [], "MONTO", xlSum, xlPie),
            ("7 Ing-Egr x mes", "Datos_Movimientos", "pt_mov_mes", ["ANIO", "MES"], ["TIPO_MOVIMIENTO"], "MONTO", xlSum, xlColumnClustered),
            ("8 Egresos x concepto", "Datos_Movimientos", "pt_mov_concepto", ["CONCEPTO"], [], "MONTO", xlSum, xlColumnClustered),
        ]
        for titulo, src, pnombre, rows, cols, data, func, chart in TABLEROS:
            ws = wb.Worksheets.Add(After=wb.Worksheets(wb.Worksheets.Count)); ws.Name = titulo
            P(ws, src, pnombre, rows, cols, data, func, chart, "A1", 320, 8)

        # ---- 3) hoja Comparar (control cruzado con Metabase) ----
        wsX = wb.Worksheets.Add(Before=wb.Worksheets(1)); wsX.Name = "Comparar"
        filas = [
            ("Metrica", "Legado (Excel/ODBC)", "Web (Metabase)"),
            ("Cuotas", "=COUNTA(Datos_Cuotas!A2:A100000)", 459),
            ("Suma cuotas (Gs.)", "=SUM(Datos_Cuotas!D2:D100000)", 1224081000),
            ("Cuotas canceladas", '=COUNTIF(Datos_Cuotas!F2:F100000,"CANCELADO")', 229),
            ("Saldo por cobrar (Gs.)", "=SUM(Datos_Cuotas!E2:E100000)", 776741670),
            ("Recaudado (Gs.)", "=SUM(Datos_Cobros!B2:B100000)", 447339330),
            ("Activos", "=COUNTA(Datos_Activos!A2:A100000)", 68),
            ("Movimientos", "=COUNTA(Datos_Movimientos!A2:A100000)", 56),
            ("Contratos", "=COUNTA(Datos_Contratos!A2:A100000)", 44),
        ]
        wsX.Range("A1").Value = "SGInmo - Cubo del legado (INMOBILIARIA.FDB por ODBC)"
        wsX.Range("A1").Font.Size = 14; wsX.Range("A1").Font.Bold = True
        for r, row in enumerate(filas):
            for c, v in enumerate(row):
                cell = wsX.Cells(r + 3, c + 1)
                cell.Value = v
                if r == 0:
                    cell.Font.Bold = True
        wsX.Range("B4:C11").NumberFormat = "#,##0"
        wsX.Columns("A:C").AutoFit()
        wsX.Range("A13").Value = "Refrescar: Datos > Actualizar todo. Las hojas de datos (ocultas) leen el .FDB por ODBC (DSN SGInmo_FDB)."

        # ---- ocultar las hojas de datos crudas (uso interno; alimentan pivots y siguen refrescando ocultas) ----
        xlSheetHidden = 0
        for nombre, _ in DATOS:
            wb.Worksheets(nombre).Visible = xlSheetHidden
        print("  hojas Datos_* ocultas")

        wb.Worksheets("Comparar").Activate()
        xl.ScreenUpdating = True
        os.makedirs(os.path.dirname(OUT), exist_ok=True)
        wb.SaveAs(OUT, FileFormat=51)  # xlsx
        print("\nLIBRO GUARDADO:", OUT)
        wb.Close(SaveChanges=False)
    finally:
        xl.Quit()
        pythoncom.CoUninitialize()


if __name__ == "__main__":
    main()
