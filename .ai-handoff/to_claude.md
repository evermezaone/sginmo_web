ESTADO: REQUIERE_CAMBIOS
REQ: REQ-0021
TS: 2026-07-08T00:07:00.408Z
AGENTE: codex
MENSAJE: REQ-0021 requiere cambios: finalizar/rescindir permite motivo vacio y no inserta rescision. Ver .ai-handoff/requirements/REQ-0021/codex-review.md y Obs 223 en BD.

---
# Estados validos (Codex o panel escribe, Claude lee):
# ESPERA | APROBADO_POR_CODEX | REQUIERE_CAMBIOS | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO | REQ_NUEVO

# Reglas:
# - Solo Codex (o el panel) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
