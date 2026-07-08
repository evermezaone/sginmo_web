# REQ-0025 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-08
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Hallazgos

### Bloqueantes

- Sin bloqueantes abiertos.

### No Bloqueantes

- `liquidacion.operacion` tiene UNIQUE en BD.
- `liquidacion_detalle` referencia `articulo`; no se reintrodujo `items_ingresos_egresos`.
- Obs 229 corregida: el guardado bloquea la operación, marca `FINALIZADO`, setea `fechaFinalizacion` y libera el activo a `LIBRE` en la misma transacción.
- Obs 230 corregida: el service y la UI exigen `motivoCodigo`.
- Obs 231 corregida parcialmente y aceptada para esta ronda: la plantilla precarga alquileres pendientes y mora calculada con `f_mora_cuota`; los demás gastos de la plantilla quedan agregables manualmente con artículos.
- Obs 232 corregida: `liquidacion_detalle.usuario_creacion` usa el usuario actual, no el literal `sistema`.
- Obs 233 corregida: `contar/listar/operacionesLiquidables/garantiaDe/plantillaDe/guardar` reciben empresa del contexto y validan pertenencia antes de calcular, listar, guardar, finalizar o liberar activo.

## Riesgos

- Contratos liquidados pero aun vigentes.
- Propiedades no liberadas despues del cierre.
- Saldos de garantia calculados sin alquileres pendientes/mora.
- Auditoria de detalles atribuida falsamente a `sistema`.
- Riesgo residual bajo: `gastosDe` no recibe empresa, pero sólo se usa desde filas ya filtradas y `guardar` valida pertenencia antes de modificar.

## Pruebas Revisadas

- [x] Revision estatica de `LiquidacionService`.
- [x] Revision estatica de `LiquidacionBean` y `liquidaciones.xhtml`.
- [x] Revision de esquema `liquidacion` / `liquidacion_detalle`.
- [x] Revision de docs `03` y `08`.
- [x] Revision de aislamiento multiempresa por analogia con REQ-0024.
- [x] `mvn -q clean package` en `Desarrollo` con JDK 23: EXIT 0.

## Pruebas Faltantes

- [ ] Prueba funcional: con dos empresas, liquidaciones debe listar/crear/editar solo operaciones de la empresa del contexto y rechazar guardar una operación ajena.
