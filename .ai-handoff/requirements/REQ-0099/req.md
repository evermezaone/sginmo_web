# REQ-0099 - Portal socio: informar transferencia mas accesible y formulario simplificado

**Numero:** REQ-0099
**Fecha de creacion:** 2026-07-14
**Estado inicial:** NUEVO
**Prioridad:** media

## Texto Original

"esta muy oculta la opcion de informar transferencia, poner donde indica la imagen en rojo (tarjeta Mis
cuotas). Para levantar/informar una transferencia bancaria debe pedir solo el monto y el adjunto. Nada mas;
el resto se infiere desde el OCR o el mail."

## Objetivo Funcional

1. Hacer visible el acceso a "Informar transferencia" con un boton prominente en la tarjeta "Mis cuotas"
   del portal (antes era un link chico en la barra superior).
2. Simplificar el formulario de informar transferencia: pedir SOLO monto + comprobante. Los demas datos
   (fecha, banco, nro de operacion, cuenta, observacion) se infieren por OCR (REQ-0084) o correo.

## Alcance

- portal/inicio.xhtml: boton "Informar transferencia" (h:link estilo boton) alineado a la derecha de la
  fila de "Periodo" en la tarjeta Mis cuotas, visible para esCliente.
- portal/transferencia.xhtml: el formulario deja solo Monto + Comprobante (se quitan fecha, banco de
  origen, cuenta de origen, nro de transaccion y observacion). Los campos removidos quedan null; el OCR
  (informar() ya corre ComprobanteOcrService best-effort) completa fecha/nro/banco.

## Criterios De Aceptacion

- [x] El boton "Informar transferencia" se ve en la tarjeta Mis cuotas (no escondido).
- [x] El formulario pide unicamente Monto y Comprobante.
- [x] Se puede informar una transferencia con solo esos dos datos.
- [x] El aislamiento y el flujo de estados (REQ-0092) siguen igual.

## Dependencias

- Base: portal/inicio.xhtml, portal/transferencia.xhtml, PortalTransferenciaBean/Service (REQ-0083/0092),
  OCR (REQ-0084).
