# REQ-0103 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-16
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Reauditoria 2026-07-16 19:44

Claude reenvio el REQ con `tools/migra_0103_exacto.py`, `tools/migra_0103_financiero.py` y `user-decision.md`.
La autorizacion destructiva queda suficientemente explicada para aceptar una carga inicial controlada, pero la
correccion no cierra todavia.

## Hallazgos

### Bloqueantes

- **Obs 325 - El script "exacto" no conserva fechas/estado historico por cuota.** `tools/migra_0103_exacto.py`
  lee `ESTADO` y `FECHA_CANCELACION` del legado (lineas 75-78), pero al insertar `cronograma_cuota` solo carga
  monto, saldo, fecha de vencimiento, moneda y documento (lineas 129-132). Luego cobra un monto agregado por
  operacion con `f_cobrar_documento` (lineas 135-139). Esa funcion llama `f_actualiza_saldo_cuotas`, que resetea
  todas las cuotas y marca canceladas por orden de numero de cuota con `fecha_cancelacion = current_date`
  (`V19__reconciliacion_cobro_gestion.sql`, lineas 29-43). Puede cuadrar totales, pero no preserva
  `fecha_cancelacion` real ni patrones no FIFO del legado. Solucion esperada: demostrar con consulta legacy-vs-web
  que cada cuota coincide en numero, monto, vencimiento, estado, saldo y fecha_cancelacion; o ajustar el proceso
  para persistir esos valores historicos exactos despues de pasar por el motor financiero, manteniendo documento,
  cobro y saldos cuadrados.

- **Obs 326 - La evidencia de preauditoria y pruebas sigue desactualizada.** `claude-implementation.md` afirma que
  se corrigio con `migra_0103_exacto.py`, pero `test-plan.md` sigue describiendo la version vieja basada en
  `f_generar_cronograma`, deja M01-M04 pendientes y no contiene pruebas del flujo exacto/documento/cobro.
  `preaudit-checklist.md` tambien sigue diciendo "Sin observaciones previas de Codex" y no documenta respuesta
  concreta por observacion cerrada. Solucion esperada: actualizar `test-plan.md` y `preaudit-checklist.md` con las
  observaciones reales, evidencia por cada correccion y consultas reproducibles de cuadre.

- **Obs 327 - No pude verificar VPS desde esta sesion.** La consulta SSH de solo lectura fallo por autenticacion
  (`Permission denied (publickey,password)`). Para aprobar una migracion de datos productiva necesito evidencia
  reproducible en archivos o acceso exitoso a conteos/sumas reales. Solucion esperada: agregar al test-plan la
  salida completa de las consultas post-carga, incluyendo conteos, sumas, documentos, cobros, saldos y comparacion
  por cuota, o asegurar que el acceso de auditoria a VPS funcione.

### No Bloqueantes

- Los scripts de migracion siguen dependiendo de rutas absolutas locales de Claude para `fbembed.dll` y scratchpad.
  No bloquea si la migracion ya fue ejecutada, pero reduce reproducibilidad.

## Pruebas Revisadas

- [x] Revision estatica de `tools/migra_0103_personas.py`.
- [x] Revision estatica de `tools/migra_0103_relacional.py`.
- [x] Revision estatica de `tools/migra_0103_financiero.py`.
- [x] Revision estatica de `tools/migra_0103_exacto.py`.
- [x] Revision de `claude-implementation.md`, `test-plan.md`, `user-decision.md` y `preaudit-checklist.md`.
- [x] `npm run handoff:check`: FAIL por REQs antiguos (`REQ-0040`, `REQ-0041`, `REQ-0042`, `REQ-0047` sin `events.jsonl`), no concluyente para REQ-0103.
- [x] Intento de consulta VPS por SSH: FAIL por autenticacion (`Permission denied (publickey,password)`).

## Pruebas Faltantes

- [ ] Comparacion legacy-vs-web por cuota: operacion, numero_cuota, fecha_vencimiento, monto, estado, saldo y fecha_cancelacion.
- [ ] Evidencia post-carga de documentos/cobros/saldos en VPS.
- [ ] `test-plan.md` y `preaudit-checklist.md` actualizados con la segunda ronda de correcciones.
