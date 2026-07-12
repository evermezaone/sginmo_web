# REQ-0056 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | Build OK | OK |
| T02 | V37 en `BEGIN...ROLLBACK` | pantalla registrada | OK |
| T03 | Deploy + Flyway V37 | success=t | OK |
| T04 | `python tools/smoke-test-vps.py` | 23/23 render OK incl. dashboard-gerencial | OK (TODAS OK) |
| T05 | Render ejercita kpis() | la pagina responde 200 (todas las native queries OK) | OK |

## Pruebas Manuales

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | Empresa con operaciones/cobros | KPIs con valores; cambio de periodo/moneda recalcula | pendiente (verificacion visual) |
| M02 | Cambiar moneda | montos cambian a esa moneda (no mezcla) | pendiente |

## Revision Transversal

- "No mezclar monedas": KPIs monetarios filtran por `cc.moneda`/`cobro.moneda`; conteos sin moneda.
- "Indicadores de mora en cuotas reales": monto/cuotas vencidas salen de cronograma_cuota (estado PENDIENTE,
  fecha_vencimiento < hoy), misma fuente que el modulo de cobros e InicioService.
- @AislarTenant -> RLS por tenant; en global (-1) devuelve cero (igual que InicioService).
- Queries sobre columnas indexadas (estado/fecha/tenant); solo lectura (no bloquea operacion).

## Datos De Prueba

Empresa real con operaciones, cuotas y cobros. Render verificado en prod (smoke).
