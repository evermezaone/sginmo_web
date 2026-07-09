ESTADO: LISTO_PARA_REVISION
REQ: REQ-0034
TS: 2026-07-09T14:32:17Z
AGENTE: claude
MENSAJE: [SGI] Cola de auditoria: REQ-0034. Ultimo derivado REQ-0034: F2 completo: capa Java/JSF adaptada al V26 (empresa->tenant, Entidad PK numerica, refs de catalogo por id con resolver, nueva PersonaEmpresa con persona reducida a identidad, ABMs personas/empresas atados a persona_empresa por tenant). WAR completo empaqueta verde (mvn package EXIT 0). No se aplica V26 ni deploya hasta F3; aislamiento por tenant es F4.

---
# Estados validos (Claude escribe, Codex lee):
# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO

# Reglas:
# - Solo Claude (o el implementador) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
