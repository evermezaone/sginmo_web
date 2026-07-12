ESTADO: LISTO_PARA_REVISION
REQ: REQ-0043, REQ-0044, REQ-0045, REQ-0046, REQ-0048, REQ-0049, REQ-0050, REQ-0051, REQ-0052, REQ-0053, REQ-0054, REQ-0055, REQ-0056, REQ-0057, REQ-0058, REQ-0059, REQ-0060, REQ-0061, REQ-0062, REQ-0063
TS: 2026-07-12T07:55:19Z
AGENTE: claude
MENSAJE: [SGI] Cola de auditoria: REQ-0043, REQ-0044, REQ-0045, REQ-0046, REQ-0048, REQ-0049, REQ-0050, REQ-0051, REQ-0052, REQ-0053, REQ-0054, REQ-0055, REQ-0056, REQ-0057, REQ-0058, REQ-0059, REQ-0060, REQ-0061, REQ-0062, REQ-0063. Ultimo derivado REQ-0063: Plantillas de roles (V44): 7 perfiles base (administrador/caja/cobrador/ventas/gerencia/auditor/consulta) con su set de permisos. RolPlantillaService: diff antes de aplicar; aplicar a un grupo escribe permiso_grupo (autorizacion REAL del backend), add-only por defecto o reemplazar explicito; NUNCA toca el perfil (no concede SUPERADMIN); verifica que el grupo sea del tenant. Auditado. Build+deploy+smoke 29/29. Diferidos: edicion de plantillas via UI, aplicar a usuario individual.

---
# Estados validos (Claude escribe, Codex lee):
# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO

# Reglas:
# - Solo Claude (o el implementador) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
