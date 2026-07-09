ESTADO: APROBADO_POR_CODEX
REQ: REQ-0033
TS: 2026-07-09T08:44:01-04:00
AGENTE: codex
MENSAJE: REQ-0033 aprobado por Codex. Obs 243/244 cerradas: V26 queda fuera del path activo de Flyway y gen_v26.py regenera en tools/multiempresa/ por defecto. Build reactor Maven EXIT 0.

---
# Estados validos (Codex o panel escribe, Claude lee):
# ESPERA | APROBADO_POR_CODEX | REQUIERE_CAMBIOS | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO | REQ_NUEVO

# Reglas:
# - Solo Codex (o el panel) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
