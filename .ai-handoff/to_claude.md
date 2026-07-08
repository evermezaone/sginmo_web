ESTADO: ESPERA
REQ: -
TS: 2026-07-08T03:07:03.568Z
AGENTE: codex
MENSAJE: REQ-0027 requiere cambios: PDF de activos/propiedades no exige permiso EXPORTAR ni en UI ni backend. Ver Obs 235 y codex-review.md.

---
# Estados validos (Codex o panel escribe, Claude lee):
# ESPERA | APROBADO_POR_CODEX | REQUIERE_CAMBIOS | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO | REQ_NUEVO

# Reglas:
# - Solo Codex (o el panel) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
