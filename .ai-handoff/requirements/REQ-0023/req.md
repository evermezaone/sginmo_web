# REQ-0023 - Descuentos y anulacion de cobros

**Estado:** implementado (2026-07-06)

## Objetivo Funcional
Anulacion de cobro que invoca f_anular_cobro (V17): repone el saldo del documento, reabre las cuotas afectadas y descuenta de la caja, todo en la BD. Boton anular en la lista de cobros de la planilla. Verificado (repone 10M/caja 0).

## Criterios De Aceptacion
- [x] Logica en la BD (SPs/triggers V16/V17), Java solo invoca.
- [x] Pantalla/accion con estandar y enforcement de permisos.
- [x] Motor verificado NUMERICAMENTE (bateria 9 checks + E2E operacion->cobro->anulacion).

## Bloqueo Formal Documentado
Validacion visual/funcional del usuario PENDIENTE (desarrollo continuo 0012-0032).
