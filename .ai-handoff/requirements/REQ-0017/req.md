# REQ-0017 - Cronograma de cuotas: generacion y edicion

**Estado:** implementado (2026-07-06)

## Objetivo Funcional
Generacion del cronograma en la BD (f_generar_cronograma, V16): reparte el total en N cuotas mensuales con dia_pago fijo; la ultima cuota absorbe el redondeo -> la suma cuadra EXACTA al total (verificado). Visible en la pestana Cronograma del detalle de operacion.

## Criterios De Aceptacion
- [x] Logica en la BD (SPs/triggers V16/V17), Java solo invoca.
- [x] Pantalla/accion con estandar y enforcement de permisos.
- [x] Motor verificado NUMERICAMENTE (bateria 9 checks + E2E operacion->cobro->anulacion).

## Bloqueo Formal Documentado
Validacion visual/funcional del usuario PENDIENTE (desarrollo continuo 0012-0032).
