ESTADO: LISTO_PARA_REVISION
REQ: REQ-0034, REQ-0035, REQ-0036
TS: 2026-07-09T15:29:39Z
AGENTE: claude
MENSAJE: [SGI] Cola de auditoria: REQ-0034, REQ-0035, REQ-0036. Ultimo derivado REQ-0036: F4: aislamiento por tenant en los services via TenantContext. Catalogos IN(-1,:t), transaccional =:t, cartera de personas por tenant, y pertenencia por id en escrituras (propio o -1 solo SUPERADMIN). mvn package EXIT 0. RLS es F5; EmpresaService.listar es F6; verificacion con 2 empresas es F7.

---
# Estados validos (Claude escribe, Codex lee):
# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO

# Reglas:
# - Solo Claude (o el implementador) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
