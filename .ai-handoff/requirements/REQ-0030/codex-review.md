# REQ-0030 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-08
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Hallazgos

### Bloqueantes

- Ninguno pendiente.

### Observaciones cerradas

- Obs 238 cerrada en ronda 2: `InicioBean` ahora inyecta `ContextoEmpresa`, obtiene la empresa del contexto y filtra todos los KPIs por `:emp`. Activos usan el criterio explicito `empresa = :emp OR empresa IS NULL`; operaciones y cobros filtran por su columna `empresa`; cuotas vencidas y saldo por cobrar unen con `operacion` para aplicar `o.empresa = :emp`.

### No Bloqueantes

- `index.xhtml` muestra los 7 KPIs pedidos.
- `index.xhtml` mantiene las tarjetas de acceso condicionadas por permisos `VER`.
- `cuotasVencidas` se renderiza con clase de alerta cuando es mayor a cero.
- `saldoPorCobrar` usa `v_operacion_saldo`, como pide el REQ.

## Riesgos

- Sin riesgos bloqueantes detectados para el alcance de REQ-0030.

## Pruebas Revisadas

- [x] Revision estatica de `InicioBean`.
- [x] Revision estatica de `index.xhtml`.
- [x] Verificacion de columnas `empresa` en `activo`, `operacion` y `cobro`.
- [x] Verificacion de `v_operacion_saldo`.
- [x] Comparacion con docs de contexto empresa/sucursal.
- [x] Build: `mvn -q clean package` en `Desarrollo` con EXIT 0.

## Pruebas Faltantes

- [ ] Prueba funcional manual con usuario de empresa A y datos de empresa B: los KPIs deben mostrar solo la empresa del contexto.
