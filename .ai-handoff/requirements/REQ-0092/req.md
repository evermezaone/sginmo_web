# REQ-0092 - Portal socio: informar transferencia con evidencia y seguimiento de estado

**Numero:** REQ-0092
**Fecha de creacion:** 2026-07-14
**Estado inicial:** NUEVO
**Prioridad:** media

## Texto Original

"...la posibilidad de levantar mi evidencia de transferencia bancaria (que queda pendiente de aprobacion y aplicacion)..."

## Objetivo Funcional

Que el socio pueda, desde su cuenta, informar una transferencia bancaria adjuntando la evidencia y ver el
estado de sus transferencias informadas (pendiente de aprobacion / en revision / aprobada / aplicada /
observada / rechazada). El backend ya existe (REQ-0083: PortalTransferenciaService.informar/mias +
portal/transferencia.xhtml); el foco de este REQ es INTEGRAR ese flujo en la vista de cuenta y hacer
visible el seguimiento de estado de forma clara.

## Alcance

- Enlazar/embeber el flujo de "informar transferencia" desde inicio.xhtml (call to action visible).
- Mostrar la lista "mis transferencias informadas" con: fecha, importe declarado, estado, evidencia
  adjunta (descarga) y, si aplica, motivo de observacion/rechazo.
- Reutiliza PortalTransferenciaService.informar()/mias() y la validacion por magic-bytes y anti-doble ya
  implementadas (REQ-0083). No re-implementar backend salvo lo necesario para exponer "mias" con estados.
- Aislamiento por persona + tenant.

## Criterios De Aceptacion

- [ ] Desde la cuenta, el socio accede a informar una transferencia y adjuntar la evidencia.
- [ ] El socio ve el estado de cada transferencia informada (pendiente/en revision/aprobada/aplicada/observada/rechazada).
- [ ] Puede descargar la evidencia que subio; ve el motivo si fue observada/rechazada.
- [ ] Un socio solo ve sus propias transferencias.

## Dependencias

- Base existente: REQ-0083 (PortalTransferenciaService, portal/transferencia.xhtml, tabla portal_pago_transferencia).
- Relacionado con REQ-0091 (panel de pagos) y REQ-0094 (conciliacion automatica).
