# Preauditoria Claude - REQ-0068

Fecha: 2026-07-12
Responsable: Claude

- [x] Lei `codex-review.md` y observaciones previas. (REQ nuevo)
- [x] Consulte `AUDITORIA_OBSERVACION` y no quedan observaciones `pendiente` para este REQ.
- [x] Observaciones cerradas marcadas con nota. (no aplica)
- [x] Documente observaciones cerradas. (no aplica)
- [x] Revise que no haya credenciales/tokens/passwords/hosts sensibles hardcodeados. (solo CSS y branding por env; ningun secreto)
- [x] `req.md` sin criterios `[ ]` pendientes salvo bloqueo documentado.
- [x] `claude-implementation.md` con Manifiesto, archivos clave, comandos probados y notas de seguridad.
- [x] `test-plan.md` solo afirma lo que existe (build/deploy/302/smoke reales; manuales de verificacion visual pendientes).
- [x] Revise flujos equivalentes: toque el flujo de login/redireccion; PORTAL y debeCambiar conservan prioridad; changeSessionId intacto.
- [x] Toque SEGURIDAD (redireccion post-login): documente el invariante anti open-redirect (solo ruta interna .xhtml).
- [x] Regla general: el return-url se valida; el branding es global pre-tenant y no expone datos por empresa.
- [x] Ejecute `python tools/handoff.py check SGI REQ-0068` y paso sin errores.

Notas:

- Riesgo medio por tocar login. Auditor: revisar `destinoGuardado()` (validacion anti open-redirect) y que el filtro solo guarde destinos GET.
- Branding configurable por `SGINMO_APP_TITULO`/`SGINMO_APP_SUBTITULO` (o -D); defaults conservan el texto actual.

## Respuesta Por Observacion Cerrada

(No aplica: REQ nuevo.)
