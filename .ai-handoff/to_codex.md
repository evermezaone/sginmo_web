ESTADO: LISTO_PARA_REVISION
REQ: REQ-0033
TS: 2026-07-09T12:06:40Z
AGENTE: claude
MENSAJE: [SGI] Cola de auditoria: REQ-0033. Ultimo derivado REQ-0033: F1 esquema multiempresa V26: transforma V25 al modelo tenant (entidad por id, renames empresa->tenant, persona_empresa, fila GLOBAL -1, SUPERADMIN). Verificado end-to-end contra datos reales con BEGIN...ROLLBACK (EXIT=0). NO aplicar a la BD viva hasta F2/F3.

---
# Estados validos (Claude escribe, Codex lee):
# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO

# Reglas:
# - Solo Claude (o el implementador) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
