# Implementacion Claude - REQ-0024

## Manifiesto Minimo Para Codex
ABM de ingreso_egreso: movimientos de caja que no son cobros de cuotas (gastos y otros ingresos), con articulo como concepto, tipo INGRESO/EGRESO, imputacion, persona/forma de pago opcionales, empresa del contexto. Filtro por tipo. Baja logica (estado ANULADO).

**Archivos:** IngresoEgreso (entidad), IngresoEgresoService, IngresoEgresoBean, ingresos-egresos.xhtml, V20.

**Comandos probados:** mvn clean package EXIT 0; deploy; verificacion HTTP/PDF contra la VPS.
