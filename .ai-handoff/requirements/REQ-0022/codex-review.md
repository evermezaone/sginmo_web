# Codex Review - REQ-0022

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-08
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Hallazgos

### Bloqueantes

- Sin bloqueantes abiertos.

### No Bloqueantes

- Obs 224 corregida en V23: `f_cobrar_documento` bloquea la planilla con `FOR UPDATE`, exige estado `ABIERTA` y valida empresa/sucursal contra el documento antes de registrar el cobro.
- Obs 225/226 corregidas en V24: `f_cobrar_documento` valida los 13 flags `requiere_*` de `forma_pago`, persiste los datos disponibles en `dato_cobro` y valida que la nota de credito asociada sea `NTCR`, no anulada y del mismo cliente. `CajaService`, `CajaBean` y `caja.xhtml` exponen y envian los campos faltantes.
- La mora se calcula en BD con `f_mora_cuota`.
- V19 reemplaza `f_cobrar_documento` con recalculo idempotente de cuotas desde el total pagado, alineado con el patron Oracle.

## Riesgos

- Riesgo residual bajo: la validacion critica queda centralizada en BD y la UI acompana los flags configurables.

## Pruebas Revisadas

- [x] Revision estatica de `CajaService`.
- [x] Revision estatica de `CajaBean` y `caja.xhtml`.
- [x] Revision estatica de `V17__motor_cobro.sql` y `V19__reconciliacion_cobro_gestion.sql`.
- [x] Revision estatica de `V23__cobro_planilla_dato_cobro.sql`.
- [x] Revision estatica de `V24__cobro_flags_completos.sql`.
- [x] Revision de esquema `forma_pago`, `dato_cobro`, `planilla`, `cobro`.
- [x] Revision de `docs-migracion/10-auditoria-gestion-oracle.md`.
- [x] `mvn -q clean package` en `Desarrollo` con JDK 23: EXIT 0.

## Pruebas Faltantes

- [ ] Prueba funcional manual en navegador: registrar cobro con una forma de pago que active todos los flags y verificar `dato_cobro`.
