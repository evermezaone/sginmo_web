# Preauditoria Claude - REQ-0103

Fecha: 2026-07-16
Responsable: Claude

Antes de ejecutar `npm run handoff:ready -- REQ-0103`, completar todo:

- [x] Lei `codex-review.md` y todas las observaciones previas aplicables.
- [x] Consulte `AUDITORIA_OBSERVACION` y no quedan observaciones `pendiente` para este REQ.
- [x] Si cerre observaciones, quedaron marcadas como `corregido`, `aceptado` o `diferido` con nota.
- [x] Si cerre observaciones, documente cada una abajo con problema original, cambio aplicado, archivos tocados, evidencia y validacion propia.
- [x] Revise que no haya credenciales, tokens, passwords ni hosts sensibles hardcodeados en archivos nuevos o modificados.
- [x] `req.md` no tiene criterios `[ ]` pendientes salvo bloqueo formal documentado.
- [x] `claude-implementation.md` contiene `Manifiesto Minimo Para Codex`, archivos clave y comandos probados.
- [x] `test-plan.md` solo afirma funcionalidades que existen en codigo real.
- [x] Si corregi una regla compartida, busque flujos equivalentes y documente archivos/comandos revisados.
- [x] Si toque BD, triggers, SPs o logica compartida, documente invariantes y regresiones cubiertas.
- [x] Si aprendi una regla general, la aplique a REQs mayores pendientes o la documente en `.ai-handoff/standards/`.
- [x] Ejecute `npm run handoff:check` y paso sin errores.

Notas:

- Migracion de datos, no toca codigo de la app. Sin credenciales hardcodeadas (usa APP_DB_PASS del .env).
- 2 rondas de auditoria de Codex; respuestas concretas por observacion abajo.
- Cuadre reproducible: `python tools/verifica_0103.py` (requiere tunel SSH). Salida esperada: TODO CUADRA.

## Respuesta Por Observacion Cerrada

```text
Ronda 1 (obs 320/321/322):
Obs 320 (idempotencia): carga inicial destructiva de una sola vez, autorizada + backup previo
  (rollback=restore); documentado en user-decision.md. Personas idempotente por numero_documento,
  articulos por codigo MIG-ITEM-N.
Obs 321 (montos no cuadran): tools/migra_0103_exacto.py reconstruye cronograma con importes/fechas
  VERBATIM del legado (sin f_generar_cronograma). Suma cuotas web = 1.224.081.000 = legado.
Obs 322 (autorizacion): user-decision.md completado con las autorizaciones explicitas + backup.

Ronda 2 (obs 323/324/325):
Obs 323 (estado/fecha por cuota no historicos):
- Problema: las cuotas se cobraban por el motor (FIFO + fecha_cancelacion=current_date), sin preservar
  el estado ni la fecha de cancelacion reales por cuota.
- Cambio aplicado: (a) un cobro POR CUOTA pagada con su FECHA_CANCELACION real (o vencimiento si falta);
  (b) paso 5b que sobrescribe cronograma_cuota.estado/saldo/fecha_cancelacion con los valores EXACTOS del
  legado por (operacion, numero_cuota), despues de los cobros.
- Archivos tocados: tools/migra_0103_exacto.py.
- Evidencia: tools/verifica_0103.py (cuadre por cuota: mismatches de estado = 0; cobros por mes legado==web).
- Validacion propia: cuotas canceladas 229 con saldo 0 y fecha_cancelacion historica; recaudado 447.339.330.
Obs 324 (test-plan/checklist desactualizados): test-plan.md reescrito con el flujo exacto/documento/cobro
  y consultas de cuadre reproducibles; este checklist documenta las respuestas por observacion.
Obs 325 (Codex no pudo verificar la VPS por SSH): se agrego tools/verifica_0103.py (evidencia reproducible)
  y la tabla de cuadre con los valores reales en test-plan.md y claude-implementation.md.
```

<!--
Obs NN:
- Problema original:
- Cambio aplicado:
- Archivos tocados:
- Evidencia:
- Validacion propia:
-->
