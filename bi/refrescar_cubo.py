# -*- coding: utf-8 -*-
"""
REQ-0107 - Refresco desatendido del cubo Excel del legado.
Abre SGInmo_Cubo_Legado.xlsx sin ventana, re-lee el FDB por ODBC (hojas Datos_*), refresca todas las
tablas dinamicas EN ORDEN (primero los datos, luego los pivots), guarda y cierra.

Pensado para agendar con el Programador de tareas de Windows, p.ej. cada 60 min:
  schtasks /Create /TN "SGInmo-Cubo-Refresco" /SC MINUTE /MO 60 ^
     /TR "\"C:\\Program Files\\Python39\\python.exe\" \"<ruta>\\bi\\refrescar_cubo.py\"" /F
(o /SC DAILY /ST 07:00 para una vez al dia). Requiere Excel instalado y el DSN SGInmo_FDB.
"""
import os, sys, time
import win32com.client as win32
import pythoncom

LIBRO = r"C:\Users\everm\SGInmoBI\SGInmo_Cubo_Legado.xlsx"


def log(msg):
    print(time.strftime("%Y-%m-%d %H:%M:%S"), msg)


def main():
    if not os.path.exists(LIBRO):
        sys.exit("No existe el libro: " + LIBRO)
    pythoncom.CoInitialize()
    xl = win32.DispatchEx("Excel.Application")
    xl.Visible = False
    xl.DisplayAlerts = False
    xl.AskToUpdateLinks = False
    try:
        # reintentar por si el archivo esta ocupado (Excel abierto / sync)
        wb = None
        for intento in range(1, 7):
            try:
                xl.Workbooks.Open(LIBRO)
                wb = xl.ActiveWorkbook   # evita el mis-wrap de win32com en el retorno de Open
                break
            except Exception:
                if intento == 6:
                    raise
                log("  archivo ocupado (intento %d), esperando 10s..." % intento)
                time.sleep(10)
        # 1) refrescar las QueryTables ODBC (hojas Datos_*) - sincrono
        n_qt = 0
        for ws in wb.Worksheets:
            for qt in ws.QueryTables:
                qt.BackgroundQuery = False
                qt.Refresh(False)
                n_qt += 1
        # 2) refrescar las tablas dinamicas (leen de las hojas ya actualizadas)
        n_pt = 0
        for ws in wb.Worksheets:
            for pt in ws.PivotTables():
                pt.RefreshTable()
                n_pt += 1
        wb.Save()
        wb.Close(SaveChanges=False)
        log("Refresco OK: %d consultas ODBC + %d tablas dinamicas -> %s" % (n_qt, n_pt, os.path.basename(LIBRO)))
    except Exception as e:
        log("ERROR en el refresco: %s" % (str(e)[:200]))
        try: wb.Close(SaveChanges=False)
        except Exception: pass
        raise
    finally:
        xl.Quit()
        pythoncom.CoUninitialize()


if __name__ == "__main__":
    main()
