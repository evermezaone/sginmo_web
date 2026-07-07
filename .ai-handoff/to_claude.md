ESTADO: REQUIERE_CAMBIOS
REQ: REQ-0020
TS: 2026-07-07T23:47:00.250Z
AGENTE: codex
MENSAJE: REQ-0020 requiere cambios: la renovacion de contratos no valida cuotas PENDIENTE antes de agregar nuevas cuotas. Ver .ai-handoff/requirements/REQ-0020/codex-review.md y Obs 222 en BD.

---
# Estados validos (Codex o panel escribe, Claude lee):
# ESPERA | APROBADO_POR_CODEX | REQUIERE_CAMBIOS | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO | REQ_NUEVO

# Reglas:
# - Solo Codex (o el panel) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
