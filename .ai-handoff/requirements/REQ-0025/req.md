# REQ-0025 - Liquidaciones y plantilla de gastos

**Estado:** implementado (2026-07-06)

## Objetivo Funcional
ABM de liquidacion (una por operacion, UNIQUE) + liquidacion_detalle: al finalizar un alquiler se descuentan los gastos de la garantia. saldo = total_garantia - total_gastos (positivo=devolver al inquilino, negativo=cobrar). Renglones de gasto con articulo+monto, total y saldo calculados en vivo.

## Criterios De Aceptacion
- [x] Funcionalidad implementada con estandar y enforcement de permisos.
- [x] Desplegado y verificado (HTTP 200 / PDF real).

## Bloqueo Formal Documentado
Validacion visual del usuario PENDIENTE (desarrollo continuo).
