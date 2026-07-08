# REQ-0024 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-08
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Hallazgos

### Bloqueantes

- Sin bloqueantes abiertos.

### No Bloqueantes

- `ingreso_egreso` referencia `articulo`; no se reintrodujo `items_ingresos_egresos`.
- El service valida articulo obligatorio, monto positivo y permisos por accion.
- Obs 228 corregida: `contar/listar` filtran por empresa del contexto; `guardar/anular` validan que el movimiento pertenezca a esa empresa, incluyendo la fila existente en BD al editar.

## Riesgos

- Riesgo residual bajo: queda pendiente validacion manual con dos empresas reales.

## Pruebas Revisadas

- [x] Revision estatica de `IngresoEgreso`.
- [x] Revision estatica de `IngresoEgresoService`.
- [x] Revision estatica de `IngresoEgresoBean` y `ingresos-egresos.xhtml`.
- [x] Revision de esquema `ingreso_egreso` y regla de `articulo` como concepto.
- [x] `mvn -q clean package` en `Desarrollo` con JDK 23: EXIT 0.

## Pruebas Faltantes

- [ ] Prueba funcional: con dos empresas, la grilla debe mostrar solo movimientos de la empresa seleccionada y bloquear editar/anular movimientos fuera de contexto.
