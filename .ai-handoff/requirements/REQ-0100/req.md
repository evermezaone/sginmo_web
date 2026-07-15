# REQ-0100 - Portal socio: volver a mi cuenta + transferencias en proceso en inicio

**Numero:** REQ-0100
**Fecha de creacion:** 2026-07-14
**Estado inicial:** NUEVO
**Prioridad:** media

## Texto Original

"El boton para volver a mi cuenta esta muy oculto arriba, agrega otro mas accesible. En la pagina principal
debe aparecer que hay una transferencia en proceso de aceptacion: fecha, monto y estado. Una vez aplicado,
ya debe aparecer al mismo nivel que cobros."

## Objetivo Funcional

1. Boton "Volver a mi cuenta" accesible en portal/transferencia.xhtml (ademas del link de la barra superior).
2. En el inicio del portal, un panel "Transferencias en proceso" que liste las transferencias informadas
   que aun NO se aplicaron (fecha, monto, estado). Las APLICADO ya figuran en "Mis pagos" (nivel de cobros).

## Alcance

- portal/transferencia.xhtml: h:link estilo boton "Volver a mi cuenta" al tope del contenido.
- PortalBean: transferenciasEnProceso = PortalTransferenciaService.mias(persona) filtrado a estado != APLICADO.
- portal/inicio.xhtml: panel (h:panelGroup rendered si hay) en el aside, arriba de "Mis pagos", con
  monto + badge de estado (getEstadoLabel) + fecha + motivo si observada/rechazada. Badge b-proceso (ambar).
- Sin cambios de BD ni backend nuevo (reusa mias() de REQ-0083/0092).

## Criterios De Aceptacion

- [x] En transferencia.xhtml hay un boton visible "Volver a mi cuenta".
- [x] En inicio aparece "Transferencias en proceso" con fecha, monto y estado cuando hay pendientes.
- [x] Una transferencia APLICADA no aparece en "en proceso" y si en "Mis pagos".
- [x] Aislamiento por persona intacto (mias filtra por persona).

## Dependencias
- Base: portal/inicio.xhtml, portal/transferencia.xhtml, PortalTransferenciaService.mias (REQ-0083/0092), REQ-0091 (panel de pagos).
