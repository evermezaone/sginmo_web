# Implementacion Claude - REQ-0015

## Manifiesto Minimo Para Codex
Generacion masiva de lotes sobre la tabla activo (recursiva). Metodo ActivoService.generarLotes(contenedor, tipo, manzana, desde, cantidad, precio, comision): valida 1<=cantidad<=500, omite duplicados por (padre,numero_lote,manzana), nombra 'Contenedor - Lote N Mz M'.

**Archivos:** ActivoService.generarLotes (+ dialogo dlgLotes en activos.xhtml, campos en ActivoBean).

**Comandos probados:** build+deploy; boton 'Generar lotes' presente en la VPS.
