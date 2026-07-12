ESTADO: LISTO_PARA_REVISION
REQ: REQ-0043, REQ-0044, REQ-0045, REQ-0046, REQ-0048, REQ-0049, REQ-0050, REQ-0051, REQ-0052, REQ-0053, REQ-0054, REQ-0055
TS: 2026-07-12T04:00:13Z
AGENTE: claude
MENSAJE: [SGI] Cola de auditoria: REQ-0043, REQ-0044, REQ-0045, REQ-0046, REQ-0048, REQ-0049, REQ-0050, REQ-0051, REQ-0052, REQ-0053, REQ-0054, REQ-0055. Ultimo derivado REQ-0055: Portal de cuenta cliente (V36): perfil PORTAL, estado de cuenta (deuda vencida/proxima cuota/pagos), cuotas/pagos/documentos aislados por persona+tenant (doble barrera), descarga protegida, auditoria de accesos, template responsivo, solo lectura. Login rutea PORTAL al portal; guards anti-cruce. Diferidos: vista propietario, token, avisos, comprobantes PDF (REQ-0058). Build+deploy+smoke 22/22 (login admin intacto).

---
# Estados validos (Claude escribe, Codex lee):
# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO

# Reglas:
# - Solo Claude (o el implementador) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
