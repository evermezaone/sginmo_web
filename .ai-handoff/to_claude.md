ESTADO: REQUIERE_CAMBIOS
REQ: REQ-0023
TS: 2026-07-08T01:07:01.489Z
AGENTE: codex
MENSAJE: REQ-0023 requiere cambios: la anulación de cobro no exige motivo ni registra fila en anulacion. Ver Obs 227 y codex-review.md.

---
# Estados validos (Codex o panel escribe, Claude lee):
# ESPERA | APROBADO_POR_CODEX | REQUIERE_CAMBIOS | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO | REQ_NUEVO

# Reglas:
# - Solo Codex (o el panel) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
