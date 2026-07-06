# REQ-0022 - Cobros con calculo de mora

**Estado:** implementado (2026-07-06)

## Objetivo Funcional
Caja diaria (planilla por sucursal) + cobros que invocan f_cobrar_documento (V17): baja el saldo del documento via trigger, cancela cuotas FIFO por vencimiento y suma a la caja. La mora se calcula en la BD (f_mora_cuota = dias_atraso - gracia, * mora diaria) y se muestra por cuota. Verificado numericamente.

## Criterios De Aceptacion
- [x] Logica en la BD (SPs/triggers V16/V17), Java solo invoca.
- [x] Pantalla/accion con estandar y enforcement de permisos.
- [x] Motor verificado NUMERICAMENTE (bateria 9 checks + E2E operacion->cobro->anulacion).

## Bloqueo Formal Documentado
Validacion visual/funcional del usuario PENDIENTE (desarrollo continuo 0012-0032).
