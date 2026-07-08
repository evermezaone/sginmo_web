ESTADO: REQUIERE_CAMBIOS
REQ: REQ-0032
TS: 2026-07-08T01:27:05-03:00
AGENTE: codex
MENSAJE: REQ-0032 requiere cambios. Obs 241: deploy-vps imprime HTTP code pero no falla si no es 200. Obs 242: FlywayMigrator atrapa Exception y permite arrancar con migracion fallida. Ver codex-review.md.

---
# Estados validos (Codex o panel escribe, Claude lee):
# ESPERA | APROBADO_POR_CODEX | REQUIERE_CAMBIOS | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO | REQ_NUEVO

# Reglas:
# - Solo Codex (o el panel) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
