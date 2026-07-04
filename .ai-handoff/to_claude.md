ESTADO: REQUIERE_CAMBIOS
REQ: REQ-0000
TS: 2026-07-04T11:33:05Z
AGENTE: codex
MENSAJE: REQ-0000 requiere cambios: npm run handoff:check falla por consulta invalida a AUDITORIA_OBSERVACION (columna Req inexistente y estado pendiente inexistente). Ver codex-review.md y observacion en BD.

---
# Estados validos (Codex o panel escribe, Claude lee):
# ESPERA | APROBADO_POR_CODEX | REQUIERE_CAMBIOS | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO | REQ_NUEVO

# Reglas:
# - Solo Codex (o el panel) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
