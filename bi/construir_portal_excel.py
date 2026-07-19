# -*- coding: utf-8 -*-
"""
REQ-0108 - Portal de preguntas en Excel (para usuarios NO expertos en tablas dinamicas).

Genera un libro con:
  - Hoja "Menu": portada con las preguntas agrupadas por ROL; cada pregunta es un hipervinculo a su hoja.
  - Una hoja por PREGUNTA: titulo + descripcion breve + vista ya agrupada/ordenada + grafico + volver al menu.
  - 5 hojas de datos ocultas (QueryTable ODBC al legado, via las vistas V_DATOS_*), refrescables.

Agregar preguntas = agregar entradas a la lista PREGUNTAS (abajo). Cada pregunta puede ser:
  - tipo "kpi"  : un numero grande (formula sobre los datos).
  - tipo "pivot": una tabla dinamica pre-armada + grafico (con filtro/orden opcionales).

Requiere: DSN ODBC SGInmo_FDB (driver Firebird 2.0.5 + cliente 2.5), Excel 32-bit, pywin32.
"""
import os
import win32com.client as win32
import pythoncom

CONN = "ODBC;DSN=SGInmo_FDB;UID=SYSDBA;PWD=masterkey;"
OUT = r"C:\Users\everm\SGInmoBI\SGInmo_Portal_Preguntas.xlsx"

xlCmdSql = 2; xlDatabase = 1
xlRowField = 1; xlColumnField = 2; xlPageField = 3; xlDataField = 4
xlSum = -4157; xlCount = -4112
xlColumnClustered = 51; xlColumnStacked = 52; xlBarClustered = 57; xlPie = 5; xlLine = 4

DATOS = [
    ("Datos_Cuotas", "SELECT * FROM V_DATOS_CUOTA"),
    ("Datos_Cobros", "SELECT * FROM V_DATOS_COBRO"),
    ("Datos_Activos", "SELECT * FROM V_DATOS_ACTIVO"),
    ("Datos_Movimientos", "SELECT * FROM V_DATOS_MOVIMIENTO"),
    ("Datos_Contratos", "SELECT * FROM V_DATOS_CONTRATO"),
]

# ── CATALOGO DE PREGUNTAS (demo). El usuario define las suyas; se agregan aca. ──
PREGUNTAS = [
    dict(id="P01", rol="Gerencia", hoja="P01_Recaudacion_total",
         pregunta="¿Cuanto se recaudo en total (historico)?",
         desc="Suma de todos los cobros registrados en el sistema.",
         tipo="kpi", formula="=SUM(Datos_Cobros!B2:B100000)", formato="#,##0"),
    dict(id="P02", rol="Gerencia", hoja="P02_Recaudacion_por_mes",
         pregunta="¿Cuanto se recaudo cada mes?",
         desc="Evolucion mensual de la recaudacion, ordenada por periodo.",
         tipo="pivot", src="Datos_Cobros", rows=["ANIO", "MES"], cols=[], data="MONTO",
         func=xlSum, chart=xlColumnClustered),
    dict(id="P03", rol="Cobranzas", hoja="P03_Mora_por_tipo",
         pregunta="¿Cuanto se debe (mora vencida) por tipo de propiedad?",
         desc="Saldo vencido agrupado por tipo de activo. Excluye lo que aun no vencio.",
         tipo="pivot", src="Datos_Cuotas", rows=["TIPO_ACTIVO"], cols=[], data="SALDO",
         func=xlSum, chart=xlBarClustered,
         filtro=("TRAMO_AGING", ["01-30", "31-60", "61-90", "90+"])),
    dict(id="P04", rol="Gerencia", hoja="P04_Ocupacion_por_tipo",
         pregunta="¿Como esta la ocupacion por tipo de propiedad?",
         desc="Unidades por situacion comercial (ocupada/vacante/vendida) y tipo.",
         tipo="pivot", src="Datos_Activos", rows=["TIPO_ACTIVO"], cols=["SITUACION"], data="ACTIVO_ID",
         func=xlCount, chart=xlColumnStacked),
]

AZUL = 0x9E5029  # BGR de #29509E
GRIS = 0xF2F2F2


def main():
    pythoncom.CoInitialize()
    xl = win32.DispatchEx("Excel.Application")
    xl.Visible = False; xl.DisplayAlerts = False; xl.ScreenUpdating = False
    try:
        wb = xl.Workbooks.Add()
        while wb.Worksheets.Count > 1:
            wb.Worksheets(wb.Worksheets.Count).Delete()

        # 1) datos ODBC (ocultos)
        rangos = {}
        for i, (nombre, sql) in enumerate(DATOS):
            ws = wb.Worksheets(1) if i == 0 else wb.Worksheets.Add(After=wb.Worksheets(wb.Worksheets.Count))
            ws.Name = nombre
            qt = ws.QueryTables.Add(Connection=CONN, Destination=ws.Range("A1"))
            qt.CommandType = xlCmdSql
            qt.CommandText = " ".join(sql.split())
            qt.BackgroundQuery = False
            qt.RefreshOnFileOpen = True
            qt.Name = "ext_" + nombre
            qt.Refresh(False)
            u = ws.UsedRange
            rangos[nombre] = (ws, u.Rows.Count, u.Columns.Count)
            for c in range(1, u.Columns.Count + 1):
                h = str(ws.Cells(1, c).Value or "").upper()
                if any(k in h for k in ("MONTO", "SALDO", "PRECIO", "GARANTIA")):
                    ws.Range(ws.Cells(2, c), ws.Cells(u.Rows.Count, c)).NumberFormat = "#,##0"
            print("  datos %-18s %d filas" % (nombre, u.Rows.Count - 1))

        # 2) hoja Menu (se llena al final con los links)
        menu = wb.Worksheets.Add(Before=wb.Worksheets(1)); menu.Name = "Menu"

        # 3) una hoja por pregunta
        for q in PREGUNTAS:
            ws = wb.Worksheets.Add(After=wb.Worksheets(wb.Worksheets.Count)); ws.Name = q["hoja"]
            # encabezado
            ws.Range("A1").Value = q["pregunta"]
            ws.Range("A1").Font.Size = 15; ws.Range("A1").Font.Bold = True; ws.Range("A1").Font.Color = AZUL
            ws.Range("A2").Value = q["desc"]
            ws.Range("A2").Font.Italic = True; ws.Range("A2").Font.Color = 0x606060
            ws.Range("A3").Value = "Rol: " + q["rol"] + "   |   Actualizado con: Datos > Actualizar todo"
            ws.Range("A3").Font.Size = 8; ws.Range("A3").Font.Color = 0x909090
            ws.Range("A4").Formula = '=HYPERLINK("#Menu!A1","◄ Volver al menu")'
            ws.Range("A4").Font.Color = AZUL

            if q["tipo"] == "kpi":
                c = ws.Range("A6"); c.Formula = q["formula"]
                c.Font.Size = 36; c.Font.Bold = True; c.Font.Color = AZUL
                c.NumberFormat = q.get("formato", "#,##0")
            else:
                ws2, nr, nc = rangos[q["src"]]
                src_range = ws2.Range(ws2.Cells(1, 1), ws2.Cells(nr, nc))
                pc = wb.PivotCaches().Create(SourceType=xlDatabase, SourceData=src_range)
                pt = pc.CreatePivotTable(TableDestination=ws.Range("A6"), TableName="pt_" + q["id"])
                try: pt.PivotCache().RefreshOnFileOpen = True
                except Exception: pass
                for f in q.get("rows", []):
                    pt.PivotFields(f).Orientation = xlRowField
                for f in q.get("cols", []):
                    pt.PivotFields(f).Orientation = xlColumnField
                if q.get("filtro"):
                    campo, permitidos = q["filtro"]
                    pf = pt.PivotFields(campo); pf.Orientation = xlPageField
                    for it in pf.PivotItems():
                        try: it.Visible = (it.Name in permitidos)
                        except Exception: pass
                df = pt.AddDataField(pt.PivotFields(q["data"]), "  " + q["data"], q["func"])
                try: df.NumberFormat = "#,##0"
                except Exception: pass
                # grafico
                if q.get("chart"):
                    try:
                        sh = ws.Shapes.AddChart2(-1, q["chart"], 330, ws.Range("A6").Top, 460, 260)
                        sh.Chart.SetSourceData(pt.TableRange1)
                        sh.Chart.HasTitle = True; sh.Chart.ChartTitle.Text = q["pregunta"]
                    except Exception as e:
                        print("     (chart %s omitido)" % q["id"])
            print("  pregunta %-24s [%s]" % (q["id"], q["rol"]))

        # 4) llenar el Menu agrupado por rol
        menu.Range("A1").Value = "SGInmo - Portal de preguntas"
        menu.Range("A1").Font.Size = 18; menu.Range("A1").Font.Bold = True; menu.Range("A1").Font.Color = AZUL
        menu.Range("A2").Value = "Elegi una pregunta y hace clic para ver la respuesta ya armada. (Actualizar: Datos > Actualizar todo)"
        menu.Range("A2").Font.Italic = True; menu.Range("A2").Font.Color = 0x707070
        fila = 4
        roles = []
        for q in PREGUNTAS:
            if q["rol"] not in roles: roles.append(q["rol"])
        for rol in roles:
            menu.Cells(fila, 1).Value = rol.upper()
            menu.Cells(fila, 1).Font.Bold = True; menu.Cells(fila, 1).Font.Size = 12
            menu.Cells(fila, 1).Interior.Color = AZUL; menu.Cells(fila, 1).Font.Color = 0xFFFFFF
            menu.Cells(fila, 2).Interior.Color = AZUL
            fila += 1
            for q in [x for x in PREGUNTAS if x["rol"] == rol]:
                cell = menu.Cells(fila, 1)
                cell.Formula = '=HYPERLINK("#%s!A1","   %s")' % (q["hoja"], q["pregunta"])
                cell.Font.Color = AZUL
                menu.Cells(fila, 2).Value = q["desc"]
                menu.Cells(fila, 2).Font.Color = 0x707070; menu.Cells(fila, 2).Font.Italic = True
                if fila % 2 == 0:
                    menu.Range(menu.Cells(fila, 1), menu.Cells(fila, 2)).Interior.Color = GRIS
                fila += 1
            fila += 1
        menu.Columns("A").ColumnWidth = 55
        menu.Columns("B").ColumnWidth = 60

        # 5) ocultar datos, activar Menu, guardar
        for nombre, _ in DATOS:
            wb.Worksheets(nombre).Visible = 0
        menu.Activate(); menu.Range("A1").Select()
        xl.ScreenUpdating = True
        os.makedirs(os.path.dirname(OUT), exist_ok=True)
        wb.SaveAs(OUT, FileFormat=51)
        wb.Close(SaveChanges=False)
        print("\nPORTAL GUARDADO:", OUT)
    finally:
        xl.Quit(); pythoncom.CoUninitialize()


if __name__ == "__main__":
    main()
