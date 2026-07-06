# Preauditoria Claude - REQ-0004

Fecha: 2026-07-06
Responsable: Claude

Antes de ejecutar `npm run handoff:ready -- REQ-0004`, completar todo:

- [x] Lei `codex-review.md` y todas las observaciones previas aplicables.
- [x] Consulte `AUDITORIA_OBSERVACION` y no quedan observaciones `pendiente` para este REQ.
- [x] Si cerre observaciones, quedaron marcadas como `corregido`, `aceptado` o `diferido` con nota. (No hubo observaciones de Codex para este REQ.)
- [x] Si cerre observaciones, documente cada una abajo con problema original, cambio aplicado, archivos tocados, evidencia y validacion propia. (N/A: sin observaciones previas.)
- [x] Revise que no haya credenciales, tokens, passwords ni hosts sensibles hardcodeados en archivos nuevos o modificados. (Credenciales en `.env` gitignorado y en `parametro_sistema` de la BD; SMTP_CLAVE aun vacia; el hash bcrypt del seed V5 es hash, no clave.)
- [x] `req.md` no tiene criterios `[ ]` pendientes salvo bloqueo formal documentado.
- [x] `claude-implementation.md` contiene `Manifiesto Minimo Para Codex`, archivos clave y comandos probados.
- [x] `test-plan.md` solo afirma funcionalidades que existen en codigo real.
- [x] Si corregi una regla compartida, busque flujos equivalentes y documente archivos/comandos revisados. (Regla "referencias absolutas desde naming containers" aplicada a articulos, usuarios y grupos; regla equals/hashCode por id aplicada a todas las entidades.)
- [x] Si toque BD, triggers, SPs o logica compartida, documente invariantes y regresiones cubiertas. (V4-V12 aplicadas a mano en VPS; invariantes: version optimista en 36 tablas, permisos efectivos = directos UNION grupos ACTIVOS, OPERAR excluye VER_AUDITORIA.)
- [x] Si aprendi una regla general, la aplique a REQs mayores pendientes o la documente en `.ai-handoff/standards/`. (Estudio completo del estandar ABM en docs-migracion/11-estandar-abm-propuesta.md, validado por el usuario 2026-07-06.)
- [x] Ejecute `npm run handoff:check` y paso sin errores. (Via tools/handoff.py ready, que lo ejecuta como gate.)

Notas:

- El modulo quedo extraido como JAR reutilizable ONEsystem-security (decision del usuario);
  el detalle de arquitectura esta en claude-implementation.md.
- Validacion FUNCIONAL del usuario (2026-07-06): "validado lo estandar" — probo login,
  permisos, ABM usuarios (reseteo de clave incluido), grupos y los ABMs de catalogo.

## Respuesta Por Observacion Cerrada

(Sin observaciones previas de Codex para este REQ.)
