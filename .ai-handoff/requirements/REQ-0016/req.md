# REQ-0016 - Operaciones alquiler-venta

**Estado:** implementado (2026-07-06)

## Objetivo Funcional
Alta de operacion de alquiler/venta: valida activo LIBRE, calcula monto total (precio*plazo en alquiler credito), crea el documento cta cte y (a credito) genera el cronograma invocando f_generar_cronograma; el activo pasa a OCUPADA/VENDIDA. Pantalla operaciones.xhtml con alta + detalle.

## Criterios De Aceptacion
- [x] Logica en la BD (SPs/triggers V16/V17), Java solo invoca.
- [x] Pantalla/accion con estandar y enforcement de permisos.
- [x] Motor verificado NUMERICAMENTE (bateria 9 checks + E2E operacion->cobro->anulacion).

## Bloqueo Formal Documentado
Validacion visual/funcional del usuario PENDIENTE (desarrollo continuo 0012-0032).
