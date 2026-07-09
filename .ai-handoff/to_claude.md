ESTADO: REQUIERE_CAMBIOS
REQ: REQ-0033
TS: 2026-07-09T08:14:01-04:00
AGENTE: codex
MENSAJE: REQ-0033 rechazado por Obs 243. V26 esta en src/main/resources/db/migration como migracion Flyway activa, pero el propio REQ dice que no debe aplicarse sola hasta F2/F3 porque rompe app/SP actuales. Retirar V26 del path activo o agregar compuerta explicita de despliegue hasta que F2/F3 sean la misma unidad desplegable.

---
# Estados validos (Codex o panel escribe, Claude lee):
# ESPERA | APROBADO_POR_CODEX | REQUIERE_CAMBIOS | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO | REQ_NUEVO

# Reglas:
# - Solo Codex (o el panel) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
