# REQ-0085 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-13
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Reauditoria De Observaciones

### Obs - Match backend movimiento/transferencia

**Estado:** corregida.

- `PortalTransferenciaService#conciliarYAplicar()` ahora usa `UPDATE movimiento_bancario_importado ... FROM portal_pago_transferencia ... RETURNING`.
- El claim exige movimiento `PENDIENTE`, transferencia en estado conciliable, mismo importe, tolerancia de fecha y referencia compatible.
- Si no retorna fila, falla antes de llamar a `aprobar()`.

### Obs - Formulario anidado CSV

**Estado:** corregida.

- `transferencias.xhtml` ya cierra el `<h:form id="frm">` antes del dialogo de movimientos.
- El dialogo `Movimientos bancarios` queda fuera del formulario principal y contiene un unico `<h:form id="frmMov" enctype="multipart/form-data">`.
- El upload CSV ya no queda en un formulario HTML anidado.

## Validacion

- Revision estatica de `PortalTransferenciaService#conciliarYAplicar()`.
- Revision estatica de `transferencias.xhtml`.
- Revision estatica de `TransferenciaBandejaBean`.
- Revision estatica de `V58__movimiento_bancario.sql`.
- Build: `mvn -q -pl sginmo-web -am clean package` desde `Desarrollo`, resultado EXIT 0.

## Hallazgos

### Bloqueantes

- Ninguno.

### No Bloqueantes

- `importarCsv()` sigue contando lineas procesadas aunque `ON CONFLICT DO NOTHING` no inserte por duplicado. Es mensaje operativo, no afecta idempotencia.

## Riesgos

- Maneja dinero, pero queda mitigado por conciliacion validada en backend, reclamo atomico del movimiento, reutilizacion del motor de caja y anti-doble aplicacion de `REQ-0083`.
