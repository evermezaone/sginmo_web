# REQ-0023 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-08
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

- Obs 227: `f_anular_cobro(p_cobro, p_usuario)` anula el cobro y sus detalles, repone saldo/cuotas y descuenta caja, pero no exige motivo ni inserta registro en `anulacion`. `docs-migracion/10-auditoria-gestion-oracle.md` define que `P_ANULARCOBRO` registra `ANULACIONES` con motivo obligatorio, y `docs-migracion/09-borrador-esquema.md` confirma que las anulaciones tienen motivo obligatorio; el esquema ya tiene tabla `anulacion` y catálogo `MOTIVOS_ANULACION`. Impacto: una reversa contable sensible queda sin trazabilidad de motivo, incumpliendo el patrón legado y la auditoría requerida.

### No Bloqueantes

- `CajaService.anularCobro` invoca el SP y no recalcula saldos en Java.
- La UI muestra el botón sólo para cobros `ACTIVO` y con permiso `caja/INACTIVAR`.
- El build Maven ejecutado durante esta ronda pasó con EXIT 0.

## Riesgos

- Anulaciones de cobro sin motivo auditable.
- Trazabilidad incompleta para reversas que afectan documento, cuotas y caja.

## Pruebas Revisadas

- [x] Revision estatica de `V19__reconciliacion_cobro_gestion.sql`.
- [x] Revision estatica de `CajaService.anularCobro`.
- [x] Revision estatica de `CajaBean.anular` y `caja.xhtml`.
- [x] Revision de docs `03`, `09` y `10` sobre anulación de cobros.
- [x] `mvn -q clean package` en `Desarrollo` con JDK 23: EXIT 0.

## Pruebas Faltantes

- [ ] Prueba funcional: anular cobro debe exigir motivo y persistir fila en `anulacion`.
