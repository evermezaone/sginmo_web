ESTADO: LISTO_PARA_REVISION
REQ: REQ-0043, REQ-0044, REQ-0045, REQ-0046, REQ-0048, REQ-0049, REQ-0050, REQ-0051, REQ-0052, REQ-0053, REQ-0054, REQ-0055, REQ-0056, REQ-0057, REQ-0058, REQ-0059, REQ-0060, REQ-0061, REQ-0062
TS: 2026-07-12T07:24:28Z
AGENTE: claude
MENSAJE: [SGI] Cola de auditoria: REQ-0043, REQ-0044, REQ-0045, REQ-0046, REQ-0048, REQ-0049, REQ-0050, REQ-0051, REQ-0052, REQ-0053, REQ-0054, REQ-0055, REQ-0056, REQ-0057, REQ-0058, REQ-0059, REQ-0060, REQ-0061, REQ-0062. Ultimo derivado REQ-0062: Reportes exportables PDF/CSV (V43): servicio comun ReportesConsultaService (generar/pdf/csv) con parametros tipados, permisos VER/EXPORTAR y limite de filas (EXPORT_LIMITE_FILAS). Sin Jasper (reutiliza PdfService/OpenPDF), CSV UTF-8. Reportes propiedades disponibles, cobros por periodo (filtro periodo/moneda, no mezcla monedas, BigDecimal, fuente transaccional) y mora. Build+deploy+smoke 28/28. Diferidos: egresos/liquidaciones (mismo patron), filtro sucursal.

---
# Estados validos (Claude escribe, Codex lee):
# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO

# Reglas:
# - Solo Claude (o el implementador) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
