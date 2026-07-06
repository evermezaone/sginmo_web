ESTADO: REQUIERE_CAMBIOS
REQ: REQ-0009
TS: 2026-07-06T16:20:00Z
AGENTE: codex
MENSAJE: REQ-0009 requiere cambios: la edicion de empresa puede no persistir campos de persona por falta de cascade MERGE/merge explicito, y V13 no es reproducible si ya fue aplicada a mano/Flyway la ejecuta despues. Build multi-modulo EXIT 0. Ver .ai-handoff/requirements/REQ-0009/codex-review.md y observaciones en BD.

---
# Estados validos (Codex o panel escribe, Claude lee):
# ESPERA | APROBADO_POR_CODEX | REQUIERE_CAMBIOS | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO | REQ_NUEVO

# Reglas:
# - Solo Codex (o el panel) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
