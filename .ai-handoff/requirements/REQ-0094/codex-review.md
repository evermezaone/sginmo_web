# REQ-0094 - Auditoria Codex

**Estado:** EN_AUDITORIA_CODEX
**Fecha:** 2026-07-14
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

- **El movimiento bancario no se consume al conciliar/aplicar el QR.** La correccion ahora marca el intento QR como `CONCILIADO` y puede auto-aplicar el cobro (`portal_pago_qr.estado='APLICADO'`), pero `QrPagoService.intentarConciliar()` no actualiza `movimiento_bancario_importado.estado_conciliacion`. El movimiento queda `PENDIENTE`, por lo que todavia aparece como candidato en `PortalTransferenciaService.candidatos()` y puede volver a usarse para aplicar una transferencia manual. Esto rompe el criterio de anti-doble aplicacion y la invariante de REQ-0085.

### No Bloqueantes

- Ninguno.

## Riesgos

- La auto-aplicacion es condicional ("gated"), pero cuando aplica debe dejar cerrada toda la cadena: intento QR, cobro y movimiento bancario. Si no se consume el movimiento, el sistema puede cobrar dos veces el mismo ingreso bancario.

## Pruebas Revisadas

- [x] Revision estatica de `QrPagoService`, `PortalTransferenciaService`, `transferencias.xhtml` y migracion V60.
- [x] Revision de respuesta a Obs 316/317 en `preaudit-checklist.md`.
- [x] Build local: `mvn -q -f Desarrollo/pom.xml -pl sginmo-web -am clean package` EXIT 0.

## Pruebas Faltantes

- [ ] Prueba manual luego del ajuste: generar intento QR, importar/recibir movimiento con referencia e importe, verificar que se crea cobro, se imputa documento/cuota, `portal_pago_qr` queda `APLICADO` con `cobro`, `movimiento_bancario_importado` queda `CONCILIADO`/consumido y ya no aparece como candidato para otra transferencia.
