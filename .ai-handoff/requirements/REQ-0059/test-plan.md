# REQ-0059 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | Build OK | OK |
| T02 | V40 en `BEGIN...ROLLBACK` | 5 columnas + pantalla | OK |
| T03 | Backup previo | dump OK | OK |
| T04 | Deploy + Flyway V40 | success=t | OK |
| T05 | `python tools/smoke-test-vps.py` (1er intento) | render OK | arqueo dio 500 (bug) |
| T06 | Diagnostico 500 | causa raiz | f:convertDateTime sin type="localDate" falla al renderizar fechas con datos |
| T07 | Fix transversal (todas las pantallas nuevas) + rebuild + smoke | 26/26 render OK | OK (TODAS OK) |

## Pruebas Manuales

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | Planilla ABIERTA con cobros -> arqueo | totales por forma de pago + esperado; cierre con confirmacion | pendiente (requiere planilla abierta con cobros) |
| M02 | Reabrir planilla cerrada | exige REACTIVAR + motivo; queda auditada | pendiente |
| M03 | Arqueo PDF | PDF con totales + esperado/contado/diferencia | pendiente |
| M04 | Modulo caja existente | apertura/cobro/anulacion/PDF recaudacion siguen OK | smoke render OK; funcional pendiente del usuario |

## Revision Transversal

- Se EXTIENDE `planilla` (ADD COLUMN IF NOT EXISTS); no se modifica CajaService (abrir/cobrar/anular/cerrar).
- Estados de planilla ABIERTA/CERRADA reutilizados (mismos valores que cerrarPlanilla).
- Fix `f:convertDateTime type=`: se reviso el patron existente (caja/operaciones/ingresos-egresos usan
  type="localDate"/"localDateTime") y se aplico a TODAS las pantallas nuevas; esto sanea un bug latente
  en REQ-0053/0054/0055/0057/0058 (fechas con datos). Verificado: `grep convertDateTime pattern=` sin type -> 0.
- Atomicidad: cerrarConArqueo y reabrir son @Transactional.

## Datos De Prueba

Empresa con planilla ABIERTA y cobros de distintas formas de pago.
