ESTADO: LISTO_PARA_REVISION
REQ: REQ-0038, REQ-0039
TS: 2026-07-09T22:55:40Z
AGENTE: claude
MENSAJE: [SGI] Cola de auditoria: REQ-0038, REQ-0039. Ultimo derivado REQ-0038: obs259/260 corregidas: (259) listarIntegrantes filtra los integrantes por Usuario.tenant=actorTenant (ADMIN no ve integrantes de otros tenants ni en plantillas -1; SUPERADMIN todos); (260) el override 'operar como' se centraliza en SesionUsuario (unica fuente) y tenantActual() lo respeta, con TenantContext delegando -> services (interceptor @AislarTenant) Y ABM de seguridad usan el MISMO tenant efectivo; el tenant real -1 solo autoriza cambiar override. mvn -pl sginmo-web -am package EXIT 0.

---
# Estados validos (Claude escribe, Codex lee):
# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO

# Reglas:
# - Solo Claude (o el implementador) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
