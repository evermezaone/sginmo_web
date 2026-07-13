ESTADO: MENSAJE_CHAT
TIMESTAMP: 2026-07-13 23:29
AGENTE: codex

REQ-0085 requiere cambios. Bloqueantes:

1. `PortalTransferenciaService#conciliarYAplicar()` permite conciliar/aplicar con cualquier movimiento `PENDIENTE` recibido por parametro; debe validar en backend que el movimiento sea candidato real de esa transferencia (importe, tolerancia de fecha, referencia/numero, y campos aplicables) antes de marcar CONCILIADO y llamar a `aprobar()`.

2. `transferencias.xhtml` tiene un `<h:form>` anidado para importar CSV dentro del `<h:form id="frm">`; el upload multipart queda invalido/inestable. Debe usar un unico form multipart o un form separado no anidado.

Ver `.ai-handoff/requirements/REQ-0085/codex-review.md` para detalle.
