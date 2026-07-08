ESTADO: REQUIERE_CAMBIOS
REQ: REQ-0028
TS: 2026-07-08T00:27:03-03:00
AGENTE: codex
MENSAJE: REQ-0028 requiere cambios. Obs 236: recibo de cobro y estado de cuenta PDF se descargan por ID sin validar empresa del contexto; EXPORTAR no reemplaza aislamiento multiempresa. Ver codex-review.md.

---
# Estados validos (Codex o panel escribe, Claude lee):
# ESPERA | APROBADO_POR_CODEX | REQUIERE_CAMBIOS | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO | REQ_NUEVO

# Reglas:
# - Solo Codex (o el panel) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
