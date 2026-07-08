# REQ-0024 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-08
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

- Obs 228: `IngresoEgresoService.contar/listar` no filtra por empresa y `IngresoEgresoBean` llama esos metodos sin pasar `ContextoEmpresa`. Aunque `nuevo()` setea `empresa` desde el contexto, la grilla muestra movimientos de todas las empresas y `editar/anular` operan por id sin validar pertenencia. Impacto: en entorno multiempresa un usuario puede ver, modificar o anular ingresos/egresos de otra empresa desde la misma pantalla, rompiendo aislamiento de datos y la regla del REQ de “empresa del contexto”.

### No Bloqueantes

- `ingreso_egreso` referencia `articulo`; no se reintrodujo `items_ingresos_egresos`.
- El service valida articulo obligatorio, monto positivo y permisos por accion.

## Riesgos

- Fuga y modificación cruzada de movimientos entre empresas.
- Reportes/caja por empresa inconsistentes si se edita o anula un movimiento fuera del contexto seleccionado.

## Pruebas Revisadas

- [x] Revision estatica de `IngresoEgreso`.
- [x] Revision estatica de `IngresoEgresoService`.
- [x] Revision estatica de `IngresoEgresoBean` y `ingresos-egresos.xhtml`.
- [x] Revision de esquema `ingreso_egreso` y regla de `articulo` como concepto.

## Pruebas Faltantes

- [ ] Reejecutar `mvn -q clean package` luego de corregir.
- [ ] Prueba funcional: con dos empresas, la grilla debe mostrar solo movimientos de la empresa seleccionada y bloquear editar/anular movimientos fuera de contexto.
