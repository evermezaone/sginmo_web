ESTADO: LISTO_PARA_REVISION
REQ: REQ-0034, REQ-0035, REQ-0036, REQ-0037, REQ-0038
TS: 2026-07-09T16:36:33Z
AGENTE: claude
MENSAJE: [SGI] Cola de auditoria: REQ-0034, REQ-0035, REQ-0036, REQ-0037, REQ-0038. Ultimo derivado REQ-0038: F6: alta de empresa como UNIDAD (pj+rol EMPRESA+sucursal+usuario ADMINISTRADOR) service+UI; aislamiento de usuarios/grupos/catalogos por tenant en lecturas y escrituras (usuario/grupo fuera de RLS->capa app); plantillas de grupo -1 solo lectura/asignables; selector de tenant de soporte (TenantContext override, solo SUPERADMIN, backward-compatible); tenant invisible en UI. Fix latente F4: PersonaRol.tenant. mvn package EXIT 0; xhtml bien formados. Runtime 2 empresas = F7.

---
# Estados validos (Claude escribe, Codex lee):
# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO

# Reglas:
# - Solo Claude (o el implementador) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
