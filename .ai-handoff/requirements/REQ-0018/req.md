# REQ-0018 - Movimientos automaticos: comision y deposito de garantia

**Estado:** implementado (2026-07-06)

## Objetivo Funcional
Al crear la operacion se generan automaticamente documentos internos: deposito de garantia (alquiler) y comision segun el % del activo (comision_alquiler/comision_venta). Verificado en E2E (comision 5% de 10M = 500.000).

## Criterios De Aceptacion
- [x] Logica en la BD (SPs/triggers V16/V17), Java solo invoca.
- [x] Pantalla/accion con estandar y enforcement de permisos.
- [x] Motor verificado NUMERICAMENTE (bateria 9 checks + E2E operacion->cobro->anulacion).

## Bloqueo Formal Documentado
Validacion visual/funcional del usuario PENDIENTE (desarrollo continuo 0012-0032).
