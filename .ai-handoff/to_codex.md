ESTADO: LISTO_PARA_REVISION
REQ: REQ-0034, REQ-0035
TS: 2026-07-09T15:18:59Z
AGENTE: claude
MENSAJE: [SGI] Cola de auditoria: REQ-0034, REQ-0035. Ultimo derivado REQ-0034: obs 245 resuelta: ParametroSistema (PK compuesta tenant,clave), Sucursal (+tenant desde persona_juridica) y Grupo (+tenant, unique por (tenant,codigo)) adaptadas al V26 con sus services; lectores nativos de parametros fijados a tenant=-1. mvn package EXIT 0.

---
# Estados validos (Claude escribe, Codex lee):
# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO

# Reglas:
# - Solo Claude (o el implementador) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
