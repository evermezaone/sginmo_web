# REQ-0085 (Fase 3) - Plan De Pruebas

**Fecha:** 2026-07-13

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | BUILD OK | OK |
| T02 | Deploy VPS + Flyway V58 + smoke | transferencias 200; 37/37; schema v58 | OK |

## Pruebas Manuales

| ID | Escenario | Pasos | Resultado esperado | Resultado real |
|---|---|---|---|---|
| M01 | Registrar/Importar | Movimientos bancarios -> registrar manual / importar CSV | Movimientos PENDIENTE cargados (import idempotente) | pendiente |
| M02 | Candidatos | Transferencia con mismo importe/fecha que un movimiento -> abrir en bandeja | Aparece el movimiento como candidato | pendiente |
| M03 | Conciliar+aplicar | Elegir documento -> Conciliar en el candidato (con caja abierta) | Movimiento CONCILIADO + cobro aplicado + APLICADO | pendiente |
| M04 | Anti-doble | Reintentar conciliar el mismo movimiento | Rechaza (ya conciliado) | pendiente |

## Datos De Prueba

Un movimiento bancario y una transferencia informada con el mismo importe.
