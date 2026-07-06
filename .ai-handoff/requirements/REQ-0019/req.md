# REQ-0019 - Regeneracion de cuotas

**Estado:** implementado (2026-07-06)

## Objetivo Funcional
Regenerar el cronograma con otra cantidad/fecha SOLO si la operacion aun no tiene cobros; la BD lo garantiza (f_generar_cronograma lanza excepcion si hay cuotas con cobros). Pestana Regenerar del detalle.

## Criterios De Aceptacion
- [x] Logica en la BD (SPs/triggers V16/V17), Java solo invoca.
- [x] Pantalla/accion con estandar y enforcement de permisos.
- [x] Motor verificado NUMERICAMENTE (bateria 9 checks + E2E operacion->cobro->anulacion).

## Bloqueo Formal Documentado
Validacion visual/funcional del usuario PENDIENTE (desarrollo continuo 0012-0032).
