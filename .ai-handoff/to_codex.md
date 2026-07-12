ESTADO: LISTO_PARA_REVISION
REQ: REQ-0043, REQ-0044, REQ-0045, REQ-0046, REQ-0048, REQ-0049, REQ-0050, REQ-0051, REQ-0052, REQ-0053, REQ-0054, REQ-0055, REQ-0056, REQ-0057, REQ-0058, REQ-0059, REQ-0060
TS: 2026-07-12T06:25:45Z
AGENTE: claude
MENSAJE: [SGI] Cola de auditoria: REQ-0043, REQ-0044, REQ-0045, REQ-0046, REQ-0048, REQ-0049, REQ-0050, REQ-0051, REQ-0052, REQ-0053, REQ-0054, REQ-0055, REQ-0056, REQ-0057, REQ-0058, REQ-0059, REQ-0060. Ultimo derivado REQ-0060: Parametrizacion avanzada (V41): extiende parametro_sistema con tipo/grupo/valor_defecto + 9 parametros iniciales globales (mora, caja obligatoria, dias alerta contrato, limite export, pie comprobante, logo, politica documental, agenda). ParametroConfig lee el valor efectivo (empresa sobre global) con cache invalidada al guardar y tolerante a mal valor. AgendaService ya lee AGENDA_DIAS_ALERTA desde parametros. Pantalla con columna Grupo. Build+deploy+smoke 26/26. Diferidos: alcance por sucursal, migracion incremental del resto de constantes.

---
# Estados validos (Claude escribe, Codex lee):
# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO

# Reglas:
# - Solo Claude (o el implementador) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
