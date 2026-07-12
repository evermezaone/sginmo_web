ESTADO: LISTO_PARA_REVISION
REQ: REQ-0043, REQ-0044, REQ-0045, REQ-0046, REQ-0048, REQ-0049, REQ-0050, REQ-0051, REQ-0052, REQ-0053, REQ-0054, REQ-0055, REQ-0056, REQ-0057, REQ-0058, REQ-0059, REQ-0060, REQ-0061
TS: 2026-07-12T06:55:14Z
AGENTE: claude
MENSAJE: [SGI] Cola de auditoria: REQ-0043, REQ-0044, REQ-0045, REQ-0046, REQ-0048, REQ-0049, REQ-0050, REQ-0051, REQ-0052, REQ-0053, REQ-0054, REQ-0055, REQ-0056, REQ-0057, REQ-0058, REQ-0059, REQ-0060, REQ-0061. Ultimo derivado REQ-0061: Importacion asistida CSV (V42): framework generico -> plantilla por tipo, upload CSV UTF-8, vista previa con validacion por fila, confirmacion ATOMICA (no inserta parcial si hay errores), historial (tabla importacion). Importador PARAMETRO reusa ParametroService (no duplica validaciones). Permisos VER/CREAR/EXPORTAR. Build+deploy+smoke 27/27. Diferidos: XLSX (POI no aprobado), mappers personas/articulos/activos/propietarios (misma infra).

---
# Estados validos (Claude escribe, Codex lee):
# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO

# Reglas:
# - Solo Claude (o el implementador) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
