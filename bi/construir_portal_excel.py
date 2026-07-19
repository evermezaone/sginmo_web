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
xlSum = -4157; xlCount = -4112; xlAverage = -4106
xlColumnClustered = 51; xlColumnStacked = 52; xlBarClustered = 57; xlPie = 5; xlLine = 4
xlDescending = 2

DATOS = [
    ("Datos_Cuotas", "SELECT * FROM V_DATOS_CUOTA"),
    ("Datos_Cobros", "SELECT * FROM V_DATOS_COBRO"),
    ("Datos_Activos", "SELECT * FROM V_DATOS_ACTIVO"),
    ("Datos_Movimientos", "SELECT * FROM V_DATOS_MOVIMIENTO"),
    ("Datos_Contratos", "SELECT * FROM V_DATOS_CONTRATO"),
]

# ── CATALOGO DE PREGUNTAS. Extensible: agregar dicts. El usuario agrega mas despues. ──
VENCIDO = ["01-30", "31-60", "61-90", "90+"]
PENDIENTE = ["POR VENCER", "01-30", "31-60", "61-90", "90+"]
PREGUNTAS = [
    # ===================== GERENCIA (visión global) =====================
    dict(id="G01", rol="Gerencia", hoja="G01_Recaudacion_total", pregunta="¿Cuánto se recaudó en total (histórico)?",
         desc="Suma de todos los cobros registrados.", tipo="kpi", formula="=SUM(Datos_Cobros!B:B)"),
    dict(id="G02", rol="Gerencia", hoja="G02_Recaudacion_mes", pregunta="¿Cuánto se recaudó cada mes?",
         desc="Evolución mensual de la recaudación.", tipo="pivot", src="Datos_Cobros",
         rows=["ANIO", "MES"], cols=[], data="MONTO", func=xlSum, chart=xlColumnClustered),
    dict(id="G03", rol="Gerencia", hoja="G03_Cartera_pendiente", pregunta="¿Cuánto falta cobrar en total (cartera)?",
         desc="Saldo pendiente de todas las cuotas.", tipo="kpi", formula="=SUM(Datos_Cuotas!E:E)"),
    dict(id="G04", rol="Gerencia", hoja="G04_Saldo_vencido", pregunta="¿Cuánto está vencido (en mora)?",
         desc="Saldo de cuotas ya vencidas y sin pagar.", tipo="kpi",
         formula='=SUMIF(Datos_Cuotas!K:K,">0",Datos_Cuotas!E:E)'),
    dict(id="G05", rol="Gerencia", hoja="G05_Pct_mora", pregunta="¿Qué porcentaje de la cartera está en mora?",
         desc="Saldo vencido dividido por la cartera pendiente.", tipo="kpi",
         formula='=SUMIF(Datos_Cuotas!K:K,">0",Datos_Cuotas!E:E)/SUM(Datos_Cuotas!E:E)', formato="0.0%"),
    dict(id="G06", rol="Gerencia", hoja="G06_Ocupacion_situacion", pregunta="¿Cómo está el parque de propiedades?",
         desc="Cantidad de unidades por situación comercial.", tipo="pivot", src="Datos_Activos",
         rows=["SITUACION"], cols=[], data="ACTIVO_ID", func=xlCount, chart=xlPie),
    dict(id="G07", rol="Gerencia", hoja="G07_Ocupacion_tipo", pregunta="¿Cómo está la ocupación por tipo de propiedad?",
         desc="Unidades por situación y tipo de activo.", tipo="pivot", src="Datos_Activos",
         rows=["TIPO_ACTIVO"], cols=["SITUACION"], data="ACTIVO_ID", func=xlCount, chart=xlColumnStacked),
    dict(id="G08", rol="Gerencia", hoja="G08_Ing_vs_egr", pregunta="¿Cómo vienen los ingresos vs egresos por mes?",
         desc="Movimientos de caja por mes y tipo.", tipo="pivot", src="Datos_Movimientos",
         rows=["ANIO", "MES"], cols=["TIPO_MOVIMIENTO"], data="MONTO", func=xlSum, chart=xlColumnClustered),
    dict(id="G09", rol="Gerencia", hoja="G09_Contratos_vigentes", pregunta="¿Cuántos contratos vigentes hay?",
         desc="Operaciones actualmente en estado vigente.", tipo="kpi",
         formula='=COUNTIF(Datos_Contratos!C:C,"VIGENTE")'),
    dict(id="G10", rol="Gerencia", hoja="G10_Valor_cartera", pregunta="¿Cuál es el valor total de la cartera de contratos?",
         desc="Suma del monto total de todas las operaciones.", tipo="kpi", formula="=SUM(Datos_Contratos!F:F)"),

    # ===================== COBRANZAS =====================
    dict(id="C01", rol="Cobranzas", hoja="C01_Aging", pregunta="¿Cómo se reparte la cartera por antigüedad (aging)?",
         desc="Saldo pendiente por tramo: por vencer, 1-30, 31-60, 61-90 y +90 días.",
         tipo="pivot", src="Datos_Cuotas", rows=["TRAMO_AGING"], cols=[], data="SALDO", func=xlSum,
         chart=xlColumnClustered, filtro=("TRAMO_AGING", PENDIENTE)),
    dict(id="C02", rol="Cobranzas", hoja="C02_Mora_tipo", pregunta="¿Cuánto se debe (mora) por tipo de propiedad?",
         desc="Saldo vencido agrupado por tipo de activo.", tipo="pivot", src="Datos_Cuotas",
         rows=["TIPO_ACTIVO"], cols=[], data="SALDO", func=xlSum, chart=xlBarClustered,
         filtro=("TRAMO_AGING", VENCIDO)),
    dict(id="C03", rol="Cobranzas", hoja="C03_Top_deudores", pregunta="¿Quiénes son los mayores deudores?",
         desc="Clientes con más saldo vencido, de mayor a menor.", tipo="pivot", src="Datos_Cuotas",
         rows=["CLIENTE"], cols=[], data="SALDO", func=xlSum, chart=xlBarClustered,
         filtro=("TRAMO_AGING", VENCIDO), orden="CLIENTE"),
    dict(id="C04", rol="Cobranzas", hoja="C04_Cuotas_vencidas", pregunta="¿Cuántas cuotas están vencidas?",
         desc="Cantidad de cuotas vencidas y sin pagar.", tipo="kpi", formula='=COUNTIF(Datos_Cuotas!K:K,">0")'),
    dict(id="C05", rol="Cobranzas", hoja="C05_Mora_mes", pregunta="¿En qué meses se concentra la mora?",
         desc="Saldo vencido por mes de vencimiento de la cuota.", tipo="pivot", src="Datos_Cuotas",
         rows=["ANIO_VTO", "MES_VTO"], cols=[], data="SALDO", func=xlSum, chart=xlColumnClustered,
         filtro=("TRAMO_AGING", VENCIDO)),
    dict(id="C06", rol="Cobranzas", hoja="C06_Cobranza_esperada", pregunta="¿Cuánto esperamos cobrar los próximos meses?",
         desc="Cuotas que aún NO vencieron, por mes de vencimiento.", tipo="pivot", src="Datos_Cuotas",
         rows=["ANIO_VTO", "MES_VTO"], cols=[], data="SALDO", func=xlSum, chart=xlColumnClustered,
         filtro=("TRAMO_AGING", ["POR VENCER"])),
    dict(id="C07", rol="Cobranzas", hoja="C07_Cuotas_estado", pregunta="¿Cómo están las cuotas por estado?",
         desc="Cantidad de cuotas canceladas vs pendientes.", tipo="pivot", src="Datos_Cuotas",
         rows=["ESTADO_CUOTA"], cols=[], data="CRONOGRAMA_CUOTA_ID", func=xlCount, chart=xlPie),

    # ===================== RECAUDACIÓN / CAJA =====================
    dict(id="R01", rol="Recaudacion", hoja="R01_Recaud_tipo", pregunta="¿De qué tipo de operación viene la recaudación?",
         desc="Cobros por tipo de operación (alquiler/venta).", tipo="pivot", src="Datos_Cobros",
         rows=["TIPO_OPERACION"], cols=[], data="MONTO", func=xlSum, chart=xlPie),
    dict(id="R02", rol="Recaudacion", hoja="R02_Recaud_cliente", pregunta="¿Qué clientes más aportan a la recaudación?",
         desc="Cobros por cliente, de mayor a menor.", tipo="pivot", src="Datos_Cobros",
         rows=["CLIENTE"], cols=[], data="MONTO", func=xlSum, chart=xlBarClustered, orden="CLIENTE"),
    dict(id="R03", rol="Recaudacion", hoja="R03_Cant_cobros", pregunta="¿Cuántos cobros se hicieron por mes?",
         desc="Cantidad de cobros registrados por mes.", tipo="pivot", src="Datos_Cobros",
         rows=["ANIO", "MES"], cols=[], data="CRONOGRAMA_CUOTA_ID", func=xlCount, chart=xlColumnClustered),
    dict(id="R04", rol="Recaudacion", hoja="R04_Recaud_propiedad", pregunta="¿Qué propiedades generan más recaudación?",
         desc="Cobros por propiedad, de mayor a menor.", tipo="pivot", src="Datos_Cobros",
         rows=["ACTIVO"], cols=[], data="MONTO", func=xlSum, chart=xlBarClustered, orden="ACTIVO"),

    # ===================== PROPIEDADES =====================
    dict(id="P01", rol="Propiedades", hoja="P01_Inventario_tipo", pregunta="¿Cuántas propiedades hay por tipo?",
         desc="Inventario del parque por tipo de activo.", tipo="pivot", src="Datos_Activos",
         rows=["TIPO_ACTIVO"], cols=[], data="ACTIVO_ID", func=xlCount, chart=xlBarClustered),
    dict(id="P02", rol="Propiedades", hoja="P02_Vacantes", pregunta="¿Cuántas unidades alquilables están vacías?",
         desc="Unidades alquilables sin contrato vigente.", tipo="kpi",
         formula='=COUNTIF(Datos_Activos!H:H,"VACANTE ALQUILABLE")'),
    dict(id="P03", rol="Propiedades", hoja="P03_Ingreso_potencial", pregunta="¿Cuánto se deja de facturar por las vacantes?",
         desc="Alquiler pretendido mensual de las unidades hoy vacantes.", tipo="kpi",
         formula='=SUMIF(Datos_Activos!H:H,"VACANTE ALQUILABLE",Datos_Activos!E:E)'),
    dict(id="P04", rol="Propiedades", hoja="P04_Alquiler_prom", pregunta="¿Cuál es el alquiler promedio por tipo?",
         desc="Precio de alquiler promedio por tipo de propiedad.", tipo="pivot", src="Datos_Activos",
         rows=["TIPO_ACTIVO"], cols=[], data="PRECIO_ALQUILER", func=xlAverage, chart=xlBarClustered),

    # ===================== CONTRATOS =====================
    dict(id="K01", rol="Contratos", hoja="K01_Por_vencer", pregunta="¿Cuántos contratos vencen en los próximos 90 días?",
         desc="Operaciones cuyo fin de contrato cae dentro de 90 días.", tipo="kpi",
         formula='=COUNTIFS(Datos_Contratos!G:G,">=0",Datos_Contratos!G:G,"<=90")'),
    dict(id="K02", rol="Contratos", hoja="K02_Tipo_estado", pregunta="¿Cómo se distribuyen los contratos por tipo y estado?",
         desc="Operaciones por tipo (alquiler/venta) y estado.", tipo="pivot", src="Datos_Contratos",
         rows=["TIPO_OPERACION"], cols=["ESTADO_OPERACION"], data="OPERACION", func=xlCount, chart=xlColumnStacked),
    dict(id="K03", rol="Contratos", hoja="K03_Nuevos_mes", pregunta="¿Cuántos contratos nuevos hubo por mes?",
         desc="Altas de operaciones por mes de la operación.", tipo="pivot", src="Datos_Contratos",
         rows=["ANIO_OP", "MES_OP"], cols=[], data="OPERACION", func=xlCount, chart=xlColumnClustered),
    dict(id="K04", rol="Contratos", hoja="K04_Avance_cobranza", pregunta="¿Cuánto se cobró vs lo pactado, por contrato?",
         desc="Monto cobrado sobre el cronograma, por operación (mayor a menor).", tipo="pivot", src="Datos_Contratos",
         rows=["ACTIVO"], cols=[], data="MONTO_COBRADO", func=xlSum, chart=xlBarClustered, orden="ACTIVO"),

    # ===================== GASTOS / EGRESOS =====================
    dict(id="E01", rol="Gastos", hoja="E01_Egresos_total", pregunta="¿Cuánto se gastó en total (egresos)?",
         desc="Suma de todos los egresos.", tipo="kpi", formula='=SUMIF(Datos_Movimientos!C:C,"EGRESO",Datos_Movimientos!E:E)'),
    dict(id="E02", rol="Gastos", hoja="E02_Egresos_concepto", pregunta="¿En qué se gasta la plata?",
         desc="Egresos por concepto, de mayor a menor.", tipo="pivot", src="Datos_Movimientos",
         rows=["CONCEPTO"], cols=[], data="MONTO", func=xlSum, chart=xlBarClustered, orden="CONCEPTO",
         filtro=("TIPO_MOVIMIENTO", ["EGRESO"])),
    dict(id="E03", rol="Gastos", hoja="E03_Egresos_mes", pregunta="¿Cómo evolucionan los egresos por mes?",
         desc="Egresos por mes.", tipo="pivot", src="Datos_Movimientos",
         rows=["ANIO", "MES"], cols=[], data="MONTO", func=xlSum, chart=xlColumnClustered,
         filtro=("TIPO_MOVIMIENTO", ["EGRESO"])),
    dict(id="E04", rol="Gastos", hoja="E04_Egresos_propiedad", pregunta="¿Qué propiedades generan más gastos?",
         desc="Egresos por propiedad, de mayor a menor.", tipo="pivot", src="Datos_Movimientos",
         rows=["ACTIVO"], cols=[], data="MONTO", func=xlSum, chart=xlBarClustered, orden="ACTIVO",
         filtro=("TIPO_MOVIMIENTO", ["EGRESO"])),
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
                dataname = "  " + q["data"]
                df = pt.AddDataField(pt.PivotFields(q["data"]), dataname, q["func"])
                try: df.NumberFormat = "#,##0"
                except Exception: pass
                if q.get("orden"):   # ordenar el campo de fila por el valor, de mayor a menor (rankings)
                    try: pt.PivotFields(q["orden"]).AutoSort(xlDescending, dataname)
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
