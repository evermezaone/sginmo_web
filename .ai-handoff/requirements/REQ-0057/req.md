# REQ-0057 - Gestion de mora, cobranza y promesas de pago

**Numero:** REQ-0057
**Fecha de creacion:** 2026-07-11
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

Proponer funcionalidades utiles y atractivas para el sistema.

## Objetivo Funcional

Agregar gestion de cobranza sobre cuotas vencidas: cartera de mora, seguimiento por responsable, promesas de pago, comentarios, proxima accion y estado de gestion.

## Criterios De Aceptacion

- [ ] Existe vista de cartera vencida con filtros por dias de mora, cliente, operacion, moneda, responsable y monto.
- [ ] Se pueden registrar gestiones de cobranza con fecha, usuario, resultado, comentario y proxima accion.
- [ ] Se puede registrar promesa de pago con fecha, monto y estado.
- [ ] Promesas vencidas aparecen como alerta/tarea.
- [ ] El cobro de una cuota relacionada actualiza o permite cerrar la promesa correspondiente.
- [ ] Permisos separados para ver cartera, gestionar, reasignar y exportar.
- [ ] Exportacion visible o filtrada respeta limites y permisos.
- [ ] Todo cambio queda auditado.

## Reglas De Negocio

- La mora debe usar la misma logica que el modulo de cobros; no duplicar calculos divergentes.
- No se debe modificar el monto original de cuota desde gestion de cobranza.
- La promesa de pago no equivale a pago ni cambia estado de cuota.

## Dependencias

- Depende de: REQ-0017, REQ-0022, REQ-0052.
- Requerido por: cobranza profesional y dashboard gerencial.

## Fuentes Y Trazabilidad

- Reglas de cobro y mora: docs-migracion de negocio y modulo de cobros.
