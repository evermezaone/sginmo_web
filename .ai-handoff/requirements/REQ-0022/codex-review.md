# Codex Review - REQ-0022

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-08
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

- Obs 224: `f_cobrar_documento` recibe `p_planilla`, pero no valida que la planilla exista, este `ABIERTA` y corresponda a la misma empresa/sucursal del documento. Inserta el `cobro` con empresa/sucursal del documento y luego suma a la planilla recibida por id. Impacto: por manipulacion o llamada directa al servicio/SP se puede registrar un cobro contra una caja cerrada o de otra sucursal, dejando caja y cobro contablemente inconsistentes.
- Obs 225: el cobro no valida los campos exigibles configurados en `forma_pago` ni inserta `dato_cobro`. El modelo ya tiene flags `requiere_*` y `dato_cobro`, y `docs-migracion/10-auditoria-gestion-oracle.md` establece que `forma_pago` parametriza los datos obligatorios al cobrar. Impacto: medios como cheque/deposito/tarjeta pueden registrarse sin numero, vencimiento, cuenta, referencia o datos de deposito aunque la forma de pago lo exija.

### No Bloqueantes

- La mora se calcula en BD con `f_mora_cuota`.
- V19 reemplaza `f_cobrar_documento` con recalculo idempotente de cuotas desde el total pagado, alineado con el patron Oracle.

## Riesgos

- Cobros asignados a planillas incorrectas o cerradas.
- Cobros sin anexos obligatorios del medio de pago, afectando conciliacion y auditoria.

## Pruebas Revisadas

- [x] Revision estatica de `CajaService`.
- [x] Revision estatica de `CajaBean` y `caja.xhtml`.
- [x] Revision estatica de `V17__motor_cobro.sql` y `V19__reconciliacion_cobro_gestion.sql`.
- [x] Revision de esquema `forma_pago`, `dato_cobro`, `planilla`, `cobro`.
- [x] Revision de `docs-migracion/10-auditoria-gestion-oracle.md`.

## Pruebas Faltantes

- [ ] Reejecutar `mvn -q clean package` luego de corregir.
- [ ] Prueba funcional: cobro con planilla cerrada u otra sucursal debe rechazar.
- [ ] Prueba funcional: forma de pago con flags `requiere_*` debe exigir y persistir `dato_cobro`.
