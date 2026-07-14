# REQ-0097 - Auditoria Codex

**Estado:** EN_AUDITORIA_CODEX
**Fecha:** 2026-07-14
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

- **No implementa el alcance ampliado de historicos.** La BD del proyecto amplió REQ-0097 para que la vista por defecto muestre el año actual + todo lo pendiente anterior, y permita consultar históricos por año y/o rango. La implementación actual de `PortalService.cuotas(Long persona)` hace `WHERE o.cliente=:p ORDER BY cc.fecha_vencimiento`, sin filtro por año/rango ni modo por defecto; `portal/inicio.xhtml` tampoco expone selector. En la práctica muestra todas las cuotas de todos los años siempre, lo cual no cumple la consulta histórica controlada pedida.

### No Bloqueantes

- Ninguno.

## Riesgos

- Si un cliente tiene varios años de alquiler, la grilla inicial puede crecer sin límite y mezclar cuotas históricas pagadas con deuda activa, justo lo que el usuario pidió evitar al definir vista por defecto + consulta histórica.

## Pruebas Revisadas

- [x] Revision estatica de `PortalService.cuotas()`, `FilaCuota` y `portal/inicio.xhtml`.
- [x] Revision del alcance ampliado registrado en BD por `sp_modificar_req` (`mysql20260714120414_update_req0097_historicos.sql`).
- [x] Build local ya ejecutado en este ciclo: `mvn -q -f Desarrollo/pom.xml -pl sginmo-web -am clean package` EXIT 0.

## Pruebas Faltantes

- [ ] Prueba manual con socio que tenga cuotas del año actual, pendientes de años anteriores y cuotas pagadas históricas.
