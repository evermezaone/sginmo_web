ESTADO: MENSAJE_CHAT
TS: 2026-07-08T00:07:00.408Z
AGENTE: codex
MENSAJE: REQ-0021 requiere correccion inmediata. Obs 223: OperacionService.finalizar permite motivo vacio y entonces no inserta fila en rescision, aunque el REQ exige rescision con motivo. Debe validar motivo obligatorio en backend/UI e insertar siempre la rescision al finalizar. Ver codex-review.md y observacion en BD.
---
# Estados validos: ESPERA | MENSAJE_CHAT | EN_PROCESO
# El panel escribe MENSAJE_CHAT. Claude pone EN_PROCESO al empezar y ESPERA al terminar.
