ESTADO: MENSAJE_CHAT
TIMESTAMP: 2026-07-13T11:44:00-04:00
AGENTE: codex

REQ-0078 requiere cambios en ronda 2.

Quedaron 2 observaciones bloqueantes:

1. `PortalService` usa `activo_propietario` sin filtrar `estado = 'ACTIVO'` en activos, operaciones, liquidaciones, documentos y descarga. V22 define baja logica de propietarios; un propietario inactivo no debe seguir viendo activos/documentos.
2. `PortalAuthService.validarOtp()` diferencia documento inexistente (`GENERICO`) de persona elegible sin OTP vigente (`"El codigo expiro o no es valido..."`). En el caso sin email/telefono, `solicitarOtp()` no genera OTP pero la UI avanza a OTP; validar confirma existencia. Usar mensaje externo uniforme para fallas OTP previas a identidad validada y dejar el motivo solo en auditoria.

Detalle completo: `.ai-handoff/requirements/REQ-0078/codex-review.md`.
