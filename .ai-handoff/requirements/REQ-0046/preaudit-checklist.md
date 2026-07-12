# Preauditoria Claude - REQ-0046

Fecha: 2026-07-11
Responsable: Claude

Antes de ejecutar `npm run handoff:ready -- REQ-0046`, completar todo:

- [x] Lei `codex-review.md` y todas las observaciones previas aplicables. (REQ nuevo, sin observaciones previas)
- [x] Consulte `AUDITORIA_OBSERVACION` y no quedan observaciones `pendiente` para este REQ.
- [x] Si cerre observaciones, quedaron marcadas como `corregido`, `aceptado` o `diferido` con nota. (no aplica: sin observaciones)
- [x] Si cerre observaciones, documente cada una abajo. (no aplica)
- [x] Revise que no haya credenciales, tokens, passwords ni hosts sensibles hardcodeados en archivos nuevos o modificados. (solo combo de moneda + query de catalogo)
- [x] `req.md` no tiene criterios `[ ]` pendientes salvo bloqueo formal documentado.
- [x] `claude-implementation.md` contiene `Manifiesto Minimo Para Codex`, archivos clave y comandos probados.
- [x] `test-plan.md` solo afirma funcionalidades que existen en codigo real.
- [x] Si corregi una regla compartida, busque flujos equivalentes y documente archivos/comandos revisados. (`monedasActivas()` sigue el patron multi-tenant de `formasHabilitadas()` en CatalogoService)
- [x] Si toque BD, triggers, SPs o logica compartida, documente invariantes y regresiones cubiertas. (sin migracion; se escribe `operacion.moneda` en altas, corrigiendo el NOT NULL; no toca operaciones existentes)
- [x] Si aprendi una regla general, la aplique a REQs mayores pendientes o la documente. (todo campo NOT NULL debe tener control en el formulario y default razonable)
- [x] Ejecute `npm run handoff:check` y paso sin errores.

Notas:

- Bug bloqueante de integridad (`null value in column moneda`) resuelto agregando el selector
  obligatorio con default Guaranies y filtrado de monedas activas por tenant.
- Riesgo residual: tenant sin ninguna Moneda ACTIVA → `required` bloquea el guardado (no
  persiste null). Mitigacion operativa: sembrar Guaranies global (tenant -1) ACTIVA.

## Respuesta Por Observacion Cerrada

(No aplica: REQ nuevo sin observaciones previas.)
