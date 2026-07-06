# REQ-0021 - Rescisiones

**Estado:** implementado (2026-07-06)

## Objetivo Funcional
Finalizar/rescindir una operacion vigente: estado FINALIZADO, el activo vuelve a LIBRE (salvo venta consumada) e inserta la rescision con motivo. Pestana Finalizar/Rescindir del detalle.

## Criterios De Aceptacion
- [x] Logica en la BD (SPs/triggers V16/V17), Java solo invoca.
- [x] Pantalla/accion con estandar y enforcement de permisos.
- [x] Motor verificado NUMERICAMENTE (bateria 9 checks + E2E operacion->cobro->anulacion).

## Bloqueo Formal Documentado
Validacion visual/funcional del usuario PENDIENTE (desarrollo continuo 0012-0032).
