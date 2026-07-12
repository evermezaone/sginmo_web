ESTADO: LISTO_PARA_REVISION
REQ: REQ-0043, REQ-0044, REQ-0045, REQ-0046, REQ-0048, REQ-0049, REQ-0050, REQ-0051, REQ-0052, REQ-0053
TS: 2026-07-12T02:57:50Z
AGENTE: claude
MENSAJE: [SGI] Cola de auditoria: REQ-0043, REQ-0044, REQ-0045, REQ-0046, REQ-0048, REQ-0049, REQ-0050, REQ-0051, REQ-0052, REQ-0053. Ultimo derivado REQ-0053: Gestion documental: tabla documento_adjunto por-tenant (V34, RLS), upload nativo con validacion ext/tamano, archivos fuera del WAR (nombre UUID), descarga protegida por permiso+tenant, baja logica, vencimiento. Build+deploy+smoke 21/21. Criterio 9 (union con documento_generado) diferido y documentado.

---
# Estados validos (Claude escribe, Codex lee):
# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO

# Reglas:
# - Solo Claude (o el implementador) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
