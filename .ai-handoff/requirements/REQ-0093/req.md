# REQ-0093 - Portal socio: pago por QR bancario Fase 1 (QR EMVCo/estatico)

**Numero:** REQ-0093
**Fecha de creacion:** 2026-07-14
**Estado inicial:** NUEVO
**Prioridad:** media

## Texto Original

"...la posibilidad de pagar por QR, que el sistema muestre un QR bancario y que le facilite la transferencia."

## Objetivo Funcional

Que el socio, al ver una cuota/saldo a pagar, pueda generar y ver en pantalla un QR bancario con los datos
del cobro (monto, cuenta destino de la empresa, concepto/identificador) para escanearlo con su app bancaria
y completar la transferencia mas rapido. Fase 1 = QR ESTATICO/EMVCo (sin conciliacion automatica): tras
pagar, el socio informa la transferencia (enlaza con REQ-0092).

## Alcance

- Parametrizar por empresa/sucursal los datos de cobro destino (cuenta, titular, banco, tipo de QR/estandar).
- Generar el QR (imagen) codificando el payload segun el estandar aplicable (evaluar EMVCo / SIPAP QR de
  Paraguay en analisis) con monto y un identificador de la cuota/documento como referencia.
- Mostrarlo en el portal junto al saldo/cuota, con instrucciones y un boton "ya transferi" que lleve a
  informar la transferencia (REQ-0092).
- Sin integracion con banco todavia; la aplicacion del pago sigue siendo manual (aprobacion en bandeja).

## Criterios De Aceptacion

- [ ] El socio puede generar/ver un QR con el monto y la referencia de la cuota a pagar.
- [ ] Los datos de la cuenta destino son parametrizables por empresa (no hardcodeados).
- [ ] El QR es escaneable por apps bancarias de plaza (validar el estandar elegido en analisis).
- [ ] Desde la misma pantalla, el socio puede pasar a informar la transferencia realizada.

## Dependencias

- Enlaza con REQ-0092 (informar transferencia).
- Requiere definir en analisis el estandar de QR (EMVCo/SIPAP) y una libreria de generacion de QR.
- Antecede a REQ-0094 (QR dinamico + conciliacion automatica).
