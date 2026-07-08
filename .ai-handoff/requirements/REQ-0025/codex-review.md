# REQ-0025 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-08
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

- Obs 229: `LiquidacionService.guardar` no finaliza la operacion ni libera el activo. `docs-migracion/03-reglas-negocio-nucleo.md` indica que al guardar liquidacion la operacion pasa a `FINALIZADO` y la propiedad a `LIBRE`; `docs-migracion/08-backlog-reqs.md` incluye explicitamente “cierre de operacion y liberacion de propiedad (transaccional)”. Impacto: se puede liquidar la garantia pero el alquiler queda `VIGENTE` y el inmueble sigue `OCUPADA`, bloqueando disponibilidad y reportes.
- Obs 230: la liquidacion no exige `motivoCodigo`, aunque RN-LIQ-003/004 define operacion y motivo de liquidacion obligatorios. El esquema tiene FK a `MOTIVOS_LIQUIDACION`, pero `motivo_codigo` queda opcional y el service no valida. Impacto: cierres de alquiler sin causa trazable.
- Obs 231: la plantilla de gastos no se genera. La UI solo permite agregar renglones manuales, pero doc 03 exige categorias precargadas y calculos automaticos para alquileres pendientes y mora (`RN-PLANT-001/002`). Impacto: una liquidacion puede omitir cuotas pendientes/mora y calcular mal el saldo garantia-gastos.
- Obs 232: los inserts nativos de `liquidacion_detalle` guardan `usuario_creacion = 'sistema'`. Eso replica el bug de auditoria hardcodeada que CODEX.md marca como no aprobable; al usar SQL nativo no actua `AuditoriaListener`. Impacto: los detalles de gastos quedan sin usuario real.

### No Bloqueantes

- `liquidacion.operacion` tiene UNIQUE en BD.
- `liquidacion_detalle` referencia `articulo`; no se reintrodujo `items_ingresos_egresos`.

## Riesgos

- Contratos liquidados pero aun vigentes.
- Propiedades no liberadas despues del cierre.
- Saldos de garantia calculados sin alquileres pendientes/mora.
- Auditoria de detalles atribuida falsamente a `sistema`.

## Pruebas Revisadas

- [x] Revision estatica de `LiquidacionService`.
- [x] Revision estatica de `LiquidacionBean` y `liquidaciones.xhtml`.
- [x] Revision de esquema `liquidacion` / `liquidacion_detalle`.
- [x] Revision de docs `03` y `08`.

## Pruebas Faltantes

- [ ] Reejecutar `mvn -q clean package` luego de corregir.
- [ ] Prueba funcional: guardar liquidacion debe finalizar operacion, liberar activo, exigir motivo, precargar/calcular plantilla y registrar usuario real en detalles.
