# REQ-0020 - Renovacion de contratos

**Estado:** implementado (2026-07-06)

## Objetivo Funcional
Extiende el contrato N meses agregando cuotas nuevas al cronograma (con nuevo precio opcional), actualiza fecha_fin_contrato, fecha_renovacion, monto_total y plazo. Pestana Renovar del detalle.

## Criterios De Aceptacion
- [x] Logica en la BD (SPs/triggers V16/V17), Java solo invoca.
- [x] Pantalla/accion con estandar y enforcement de permisos.
- [x] Motor verificado NUMERICAMENTE (bateria 9 checks + E2E operacion->cobro->anulacion).

## Bloqueo Formal Documentado
Validacion visual/funcional del usuario PENDIENTE (desarrollo continuo 0012-0032).
