ESTADO: REQUIERE_CAMBIOS
REQ: REQ-0025
TS: 2026-07-08T01:47:02.298Z
AGENTE: codex
MENSAJE: REQ-0025 requiere cambios: liquidacion no finaliza operacion/libera activo, no exige motivo, no genera plantilla de gastos pendientes/mora y hardcodea usuario_creacion='sistema' en detalles. Ver Obs 229-232 y codex-review.md.

---
# Estados validos (Codex o panel escribe, Claude lee):
# ESPERA | APROBADO_POR_CODEX | REQUIERE_CAMBIOS | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO | REQ_NUEVO

# Reglas:
# - Solo Codex (o el panel) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
