# REQ-0030 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-08
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

- Obs 238: el dashboard suma indicadores globales sin filtrar por empresa del contexto. `InicioBean.iniciar()` ejecuta agregaciones directas sobre `activo`, `operacion`, `cronograma_cuota`, `cobro` y `v_operacion_saldo` sin `ContextoEmpresa` ni `empresa = :emp`. El esquema tiene `activo.empresa`, `operacion.empresa` y `cobro.empresa`; el estandar del stack exige mapear `VARIABLES_ENTORNOS` a sesion y aplicar filtro por `empresa_id` en consultas. Impacto: un usuario ve KPIs agregados de otras empresas (activos, operaciones, recaudacion del dia y saldo por cobrar), filtracion de datos de negocio en la portada.

### No Bloqueantes

- `index.xhtml` muestra los 7 KPIs pedidos.
- `index.xhtml` mantiene las tarjetas de acceso condicionadas por permisos `VER`.
- `cuotasVencidas` se renderiza con clase de alerta cuando es mayor a cero.
- `saldoPorCobrar` usa `v_operacion_saldo`, como pide el REQ.

## Riesgos

- Fuga de informacion interempresa por indicadores agregados.

## Pruebas Revisadas

- [x] Revision estatica de `InicioBean`.
- [x] Revision estatica de `index.xhtml`.
- [x] Verificacion de columnas `empresa` en `activo`, `operacion` y `cobro`.
- [x] Verificacion de `v_operacion_saldo`.
- [x] Comparacion con docs de contexto empresa/sucursal.

## Pruebas Faltantes

- [ ] Reejecutar `mvn -q clean package` luego de corregir.
- [ ] Prueba funcional con usuario de empresa A y datos de empresa B: los KPIs deben mostrar solo la empresa del contexto.

## Solucion Esperada

- Inyectar `ContextoEmpresa` o resolver la empresa actual desde el usuario autenticado.
- Filtrar todos los KPIs por empresa del contexto:
  - activos por `activo.empresa = :emp` (definir explicitamente si los activos `empresa IS NULL` son globales o deben excluirse).
  - operaciones vigentes por `operacion.empresa = :emp`.
  - cuotas vencidas uniendo `cronograma_cuota` con `operacion` y filtrando `operacion.empresa = :emp`.
  - recaudado hoy por `cobro.empresa = :emp`.
  - saldo por cobrar uniendo `v_operacion_saldo` con `operacion` y filtrando `operacion.empresa = :emp`.
