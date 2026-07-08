# Codex Review - REQ-0022

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-08
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

- Obs 226: la correccion de Obs 225 quedo parcial. `V23__cobro_planilla_dato_cobro.sql`, `CajaService`, `CajaBean` y `caja.xhtml` solo soportan emisor, procesador, numero, serie, cuenta, vencimiento y referencia. El modelo de `forma_pago` tambien tiene `requiere_cobrador`, `requiere_fecha_deposito`, `requiere_numero_deposito`, `requiere_estado_deposito`, `requiere_motivo_rechazo` y `requiere_nota_credito`, con sus columnas destino en `dato_cobro` (`cobrador`, `fecha_deposito`, `numero_deposito`, `estado_deposito_codigo`, `motivo_rechazo_codigo`, `ntcr_documento`). Impacto: una forma de pago configurada para deposito, rechazo, cobrador o nota de credito puede cobrarse sin exigir ni persistir esos datos, incumpliendo la regla solicitada de campos exigibles segun configuracion.

### No Bloqueantes

- Obs 224 corregida en V23: `f_cobrar_documento` bloquea la planilla con `FOR UPDATE`, exige estado `ABIERTA` y valida empresa/sucursal contra el documento antes de registrar el cobro.
- La mora se calcula en BD con `f_mora_cuota`.
- V19 reemplaza `f_cobrar_documento` con recalculo idempotente de cuotas desde el total pagado, alineado con el patron Oracle.

## Riesgos

- Cobros sin anexos obligatorios del medio de pago, afectando conciliacion y auditoria.

## Pruebas Revisadas

- [x] Revision estatica de `CajaService`.
- [x] Revision estatica de `CajaBean` y `caja.xhtml`.
- [x] Revision estatica de `V17__motor_cobro.sql` y `V19__reconciliacion_cobro_gestion.sql`.
- [x] Revision estatica de `V23__cobro_planilla_dato_cobro.sql`.
- [x] Revision de esquema `forma_pago`, `dato_cobro`, `planilla`, `cobro`.
- [x] Revision de `docs-migracion/10-auditoria-gestion-oracle.md`.

## Pruebas Faltantes

- [ ] Reejecutar `mvn -q clean package` luego de corregir.
- [ ] Prueba funcional: forma de pago con todos los flags `requiere_*` debe exigir y persistir `dato_cobro`.
