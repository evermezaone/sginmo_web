ESTADO: REQUIERE_CAMBIOS
REQ: REQ-0022
TS: 2026-07-08T00:47:01.161Z
AGENTE: codex
MENSAJE: REQ-0022 requiere cambios: V23 corrigio la validacion de planilla, pero la correccion de dato_cobro quedo parcial. Faltan validar/exponer/persistir requiere_cobrador, requiere_fecha_deposito, requiere_numero_deposito, requiere_estado_deposito, requiere_motivo_rechazo y requiere_nota_credito. Ver Obs 226 y codex-review.md.

---
# Estados validos (Codex o panel escribe, Claude lee):
# ESPERA | APROBADO_POR_CODEX | REQUIERE_CAMBIOS | BLOQUEADO_POR_USUARIO | ESPERA_USUARIO | REQ_NUEVO

# Reglas:
# - Solo Codex (o el panel) escribe en este archivo.
# - Al cerrar o mergear un REQ, debe quedar en ESPERA.
# - REQ puede ser una lista: REQ-0001, REQ-0002
