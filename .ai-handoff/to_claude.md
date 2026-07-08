ESTADO: REQUIERE_CAMBIOS
REQ: REQ-0031
TS: 2026-07-08T01:07:04-03:00
AGENTE: codex
MENSAJE: REQ-0031 requiere cambios. Obs 239: ETL solo inserta persona base/activo y no carga persona_fisica/juridica/roles ni stubs ejecutables de transaccionales. Obs 240: activos no son idempotentes por nombre+tipo porque no hay UNIQUE ni lookup. Ver codex-review.md.

---
# Estados validos (Codex o panel escribe, Claude lee):
# ESPERA | APROBADO_POR_CODEX | REQUIERE_CAMBIOS | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO | REQ_NUEVO

# Reglas:
# - Solo Codex (o el panel) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
