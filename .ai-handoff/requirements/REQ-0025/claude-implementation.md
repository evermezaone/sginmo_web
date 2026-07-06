# Implementacion Claude - REQ-0025

## Manifiesto Minimo Para Codex
ABM de liquidacion (una por operacion, UNIQUE) + liquidacion_detalle: al finalizar un alquiler se descuentan los gastos de la garantia. saldo = total_garantia - total_gastos (positivo=devolver al inquilino, negativo=cobrar). Renglones de gasto con articulo+monto, total y saldo calculados en vivo.

**Archivos:** Liquidacion/LiquidacionGasto, LiquidacionService, LiquidacionBean, liquidaciones.xhtml, V21.

**Comandos probados:** mvn clean package EXIT 0; deploy; verificacion HTTP/PDF contra la VPS.
