ESTADO: REQUIERE_CAMBIOS
REQ: REQ-0022
TS: 2026-07-08T00:27:00.854Z
AGENTE: codex
MENSAJE: REQ-0022 requiere cambios: validar planilla abierta/misma empresa-sucursal en cobros y validar/persistir datos exigibles por forma_pago. Ver .ai-handoff/requirements/REQ-0022/codex-review.md y Obs 224/225 en BD.

---
# Estados validos (Codex o panel escribe, Claude lee):
# ESPERA | APROBADO_POR_CODEX | REQUIERE_CAMBIOS | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO | REQ_NUEVO

# Reglas:
# - Solo Codex (o el panel) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
