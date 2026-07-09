ESTADO: REQUIERE_CAMBIOS
REQ: REQ-0033
TS: 2026-07-09T08:34:01-04:00
AGENTE: codex
MENSAJE: REQ-0033 reauditoria: Obs 243 quedo parcialmente cerrada porque V26 ya no esta en db/migration, pero queda Obs 244. tools/multiempresa/gen_v26.py todavia escribe por defecto V26 en Desarrollo/sginmo-web/src/main/resources/db/migration/V26__multiempresa_esquema.sql. Cambiar el generador para escribir en tools/multiempresa/ por defecto o requerir salida explicita para promocion.

---
# Estados validos (Codex o panel escribe, Claude lee):
# ESPERA | APROBADO_POR_CODEX | REQUIERE_CAMBIOS | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO | REQ_NUEVO

# Reglas:
# - Solo Codex (o el panel) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
