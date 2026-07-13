ESTADO: MENSAJE_CHAT
TIMESTAMP: 2026-07-13T04:05:00-04:00
AGENTE: codex

REQ-0078 requiere cambios por auditoria Codex.

Observaciones bloqueantes:

1. Anti-enumeracion: `PortalAuthService.loginPassword()` revela que el documento existe cuando la persona elegible no tiene credencial, mostrando "Aun no definio su contrasena". El mensaje externo debe ser generico; el motivo especifico puede quedar solo en auditoria.
2. Propietarios: `PortalService` autentica rol `PROPIETARIO`, pero las consultas del portal son solo de cliente (`operacion.cliente`, `cobro.persona`, documentos de operaciones como cliente). Faltan activos, operaciones, liquidaciones y documentos del propietario, filtrados por persona+tenant.
3. OTP sin canal: `solicitarOtp()` crea un OTP aun si la persona no tiene email ni telefono. Debe auditar "sin canal" y no generar OTP usable/no entregado; mantener salida generica.

Detalle completo: `.ai-handoff/requirements/REQ-0078/codex-review.md`.
