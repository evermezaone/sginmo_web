# Preauditoria Claude - REQ-0000

Fecha: 2026-07-04
Responsable: Claude

Antes de ejecutar la compuerta (`python tools/handoff.py ready SGI REQ-0000`), completar todo:

- [x] Lei `codex-review.md` y todas las observaciones previas aplicables. (Primera entrega.)
- [x] Consulte `AUDITORIA_OBSERVACION` y no quedan observaciones `pendiente` para este REQ.
- [x] Si cerre observaciones, quedaron marcadas con nota. (No aplica.)
- [x] Si cerre observaciones, documente cada una abajo. (No aplica.)
- [x] Revise que no haya credenciales, tokens, passwords ni hosts sensibles hardcodeados en archivos nuevos o modificados versionados. (Credenciales solo en `.env`/`tmp_my.cnf`, ambos gitignoreados y verificados con `git ls-files`; la contrasena de la VPS no se almaceno en ningun lado.)
- [x] `req.md` no tiene criterios `[ ]` pendientes salvo bloqueo formal documentado.
- [x] `claude-implementation.md` contiene `Manifiesto Minimo Para Codex`, archivos clave y comandos probados.
- [x] `test-plan.md` solo afirma funcionalidades que existen/ocurrieron realmente.
- [x] Si corregi una regla compartida, busque flujos equivalentes. (No aplica.)
- [x] Si toque BD, triggers, SPs o logica compartida, documente invariantes. (No aplica.)
- [x] Si aprendi una regla general, la aplique o documente. (Servicios preexistentes de la VPS anotados como restriccion para REQ-0032.)
- [x] Ejecute la compuerta de check y paso sin errores.

Notas:

- El acceso VPS quedo por clave dedicada; la contrasena nunca se guardo (opcion A elegida por el usuario).

## Respuesta Por Observacion Cerrada

Obs 201 (criterio-no-cumplido / handoff-check-auditoria-observacion, alta):
- Problema original: `tools/handoff-check.js` y `tools/handoff-ready.js` consultaban `AUDITORIA_OBSERVACION.Req` y `Estado='pendiente'`, inexistentes en el esquema real (usa `IdReq` y estados `abierta/corregida/descartada/diferida`); `npm run handoff:check` fallaba con "Unknown column 'Req'".
- Cambio aplicado: consulta corregida en ambos tools: JOIN `AUDITORIA_OBSERVACION.IdReq -> REQ.IdReq -> PROYECTO.IdProyecto`, filtro `PROYECTO.Codigo = PROJECT_CODE` (nueva validacion de presencia en .env), `REQ.Codigo IN (...)` y `Estado='abierta'`; mensajes actualizados a "observacion ABIERTA".
- Archivos tocados: `tools/handoff-check.js`, `tools/handoff-ready.js`.
- Evidencia: `npm run handoff:check` → `HANDOFF CHECK: OK` EXIT:0 (ver test-plan T07); revision transversal: `grep AUDITORIA_OBSERVACION tools/` — los 3 consumidores (handoff-check.js, handoff-ready.js, handoff.py) usan el mismo patron IdReq/abierta; no quedan otros usos.
- Validacion propia: la compuerta Python (`tools/handoff.py check SGI REQ-0000`) detectaba la Obs 201 como ABIERTA antes del cierre, confirmando que ambas compuertas leen la misma fuente.

Hallazgo no bloqueante (claude-plan.md como plantilla): completado con la estrategia y archivos reales.
