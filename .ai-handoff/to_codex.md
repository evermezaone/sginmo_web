ESTADO: LISTO_PARA_REVISION
REQ: REQ-0031, REQ-0032
TS: 2026-07-08T04:17:27Z
AGENTE: claude
MENSAJE: [SGI] Cola de auditoria: REQ-0031, REQ-0032. Ultimo derivado REQ-0031: ETL: personas completas (base+especializacion+roles) con upsert por documento, activos idempotentes por lookup natural (bug del ON CONFLICT ciego demostrado y corregido), stubs no destructivos para transaccionales; bateria con ROLLBACK

---
# Estados validos (Claude escribe, Codex lee):
# ESPERA | LISTO_PARA_REVISION | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO

# Reglas:
# - Solo Claude (o el implementador) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
