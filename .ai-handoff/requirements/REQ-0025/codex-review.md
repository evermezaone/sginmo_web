# REQ-0025 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-08
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

- Obs 233: `LiquidacionService.contar/listar/operacionesLiquidables/garantiaDe/guardar` no reciben ni validan empresa del contexto, y `LiquidacionBean` no inyecta `ContextoEmpresa`. La pantalla lista liquidaciones y operaciones liquidables de todas las empresas; además, una llamada manipulada puede guardar una liquidación de una operación ajena, finalizarla y liberar su activo. Impacto: fuga y modificación cruzada de contratos/activos entre empresas, más grave que en REQ-0024 porque cierra operaciones y cambia disponibilidad del inmueble.

### No Bloqueantes

- `liquidacion.operacion` tiene UNIQUE en BD.
- `liquidacion_detalle` referencia `articulo`; no se reintrodujo `items_ingresos_egresos`.
- Obs 229 corregida: el guardado bloquea la operación, marca `FINALIZADO`, setea `fechaFinalizacion` y libera el activo a `LIBRE` en la misma transacción.
- Obs 230 corregida: el service y la UI exigen `motivoCodigo`.
- Obs 231 corregida parcialmente y aceptada para esta ronda: la plantilla precarga alquileres pendientes y mora calculada con `f_mora_cuota`; los demás gastos de la plantilla quedan agregables manualmente con artículos.
- Obs 232 corregida: `liquidacion_detalle.usuario_creacion` usa el usuario actual, no el literal `sistema`.

## Riesgos

- Contratos liquidados pero aun vigentes.
- Propiedades no liberadas despues del cierre.
- Saldos de garantia calculados sin alquileres pendientes/mora.
- Auditoria de detalles atribuida falsamente a `sistema`.
- Liquidaciones cruzadas entre empresas si no se aísla por contexto.

## Pruebas Revisadas

- [x] Revision estatica de `LiquidacionService`.
- [x] Revision estatica de `LiquidacionBean` y `liquidaciones.xhtml`.
- [x] Revision de esquema `liquidacion` / `liquidacion_detalle`.
- [x] Revision de docs `03` y `08`.
- [x] Revision de aislamiento multiempresa por analogia con REQ-0024.

## Pruebas Faltantes

- [ ] Reejecutar `mvn -q clean package` luego de corregir.
- [ ] Prueba funcional: con dos empresas, liquidaciones debe listar/crear/editar solo operaciones de la empresa del contexto y rechazar guardar una operación ajena.
