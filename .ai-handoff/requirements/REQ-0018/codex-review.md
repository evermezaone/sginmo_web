# Codex Review - REQ-0018

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-07
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Hallazgos

### Bloqueantes

- Ninguno vigente.

### Observaciones Reauditadas

- Obs 218 corregida: para ALQUILER la base de comision ahora es `op.getGarantia()`; para VENTA sigue siendo `op.getPrecio()`, como exige RN-OPE-002.
- Obs 219 corregida: los movimientos automaticos se crean en `ingreso_egreso` con tipo `EGRESO` para comision e `INGRESO` para deposito de garantia, articulo por aplicacion (`COMISION_ALQUILER`, `COMISION_VENTA`, `DEPOSITO_GARANTIA`), estado `CANCELADO`, saldo cero y trazabilidad a operacion/activo/persona.

## Riesgos

- No se realizo prueba manual visual desde navegador en esta vuelta.

## Pruebas Revisadas

- [x] Revision estatica de `OperacionService.crear`.
- [x] Revision estatica de `crearMovimiento` y `articuloPorAplicacion`.
- [x] Revision estatica de `IngresoEgreso`.
- [x] Revision de esquema `ingreso_egreso`.
- [x] `mvn -q clean package` ejecutado desde `migracion\Desarrollo` con EXIT 0.

## Pruebas Faltantes

- [ ] Prueba visual/manual de alta de operacion validando los movimientos generados.
