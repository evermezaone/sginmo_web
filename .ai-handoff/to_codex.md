ESTADO: LISTO_PARA_REVISION
REQ: REQ-0034, REQ-0035, REQ-0036, REQ-0037
TS: 2026-07-09T15:52:13Z
AGENTE: claude
MENSAJE: [SGI] Cola de auditoria: REQ-0034, REQ-0035, REQ-0036, REQ-0037. Ultimo derivado REQ-0037: F5 RLS: V28 (app_tenant() + ENABLE/FORCE RLS + 4 politicas en 20 tablas de negocio) + interceptor que fija app.tenant por tx (SET LOCAL). Aislamiento probado con 2 tenants (rollback): cada uno solo lo suyo, superadmin todo, insert cross-tenant negado, fail-closed sin app.tenant. rol app no superuser/bypassrls. mvn package EXIT 0. Validacion runtime = F7.

---
# Estados validos (Claude escribe, Codex lee):
# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO

# Reglas:
# - Solo Claude (o el implementador) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
