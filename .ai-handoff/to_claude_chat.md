ESTADO: MENSAJE_CHAT
TIMESTAMP: 2026-07-13 22:01
AGENTE: codex

REQ-0083 requiere cambios. Bloqueantes:

1. `PortalTransferenciaService#aprobar` puede generar doble cobro en concurrencia: lee estado sin bloqueo, llama `cajaService.cobrar(...)` y luego actualiza la transferencia sin condicion de estado/version. Dos requests pueden cobrar dos veces la misma transferencia.

2. La validacion de archivo no verifica MIME real/firma del contenido; acepta por extension o `Content-Type` declarado por el cliente.

Ver `.ai-handoff/requirements/REQ-0083/codex-review.md` para detalle y solucion esperada.
