# REQ-0094 - Auditoria Codex

**Estado:** EN_AUDITORIA_CODEX
**Fecha:** 2026-07-14
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

- **No aplica el cobro automaticamente.** El REQ exige que el pago QR, al confirmarse, se concilie y se aplique automaticamente sin informe manual ni aprobacion. La implementacion solo marca `portal_pago_qr.estado='CONCILIADO'` y lo muestra como "listo para aplicar"; `QrPagoService.intentarConciliar()` no llama al motor de caja, no setea `cobro`, no pasa el intento a `APLICADO`, y `transferencias.xhtml` indica al operador "Aplica el cobro por el flujo habitual de caja". Esto incumple los criterios de aceptacion principales.
- **La conciliacion puede tomar intentos QR vencidos.** `referenciaIntento()` reusa solo intentos no vencidos, pero `intentarConciliar()` busca cualquier `estado='PENDIENTE'` por referencia e importe sin validar `expira_en`. Un movimiento bancario tardio puede conciliar un QR expirado, contrariando el alcance de "manejo de estados y expiracion del QR".

### No Bloqueantes

- Ninguno.

## Riesgos

- La auto-aplicacion requiere regla de imputacion, caja/planilla y forma de pago. Si aun no existe definicion de negocio, el REQ debe pasar a `BLOQUEADO_POR_USUARIO` o modificarse formalmente; no corresponde aprobarlo como si el criterio estuviera implementado.

## Pruebas Revisadas

- [x] Revision estatica de `QrPagoService`, `PortalTransferenciaService`, `transferencias.xhtml` y migracion V60.
- [x] Revision de `claude-implementation.md`, `preaudit-checklist.md` y `user-decision.md`; no hay decision de usuario registrada que recorte el alcance a "base ahora, auto-apply despues".

## Pruebas Faltantes

- [ ] Prueba manual luego del ajuste: generar intento QR, importar/recibir movimiento con referencia e importe, verificar que se crea cobro, se imputa documento/cuota, `portal_pago_qr` queda `APLICADO` con `cobro`, el movimiento queda consumido y el panel del socio lo muestra como aplicado.
