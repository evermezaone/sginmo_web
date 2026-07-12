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

- [x] Existe vista de cartera vencida con filtros por dias de mora, cliente, operacion, moneda, responsable y monto. (cartera con filtros dias/monto/moneda en UI; cliente/operacion soportados en el servicio; responsable aplica a la gestion)
- [x] Se pueden registrar gestiones de cobranza con fecha, usuario, resultado, comentario y proxima accion. (tabla gestion_cobranza)
- [x] Se puede registrar promesa de pago con fecha, monto y estado. (tabla promesa_pago)
- [x] Promesas vencidas aparecen como alerta/tarea. (AgendaService.generarAutomaticos crea evento PROMESA para promesas PENDIENTE vencidas, con dedup)
- [x] El cobro de una cuota relacionada actualiza o permite cerrar la promesa correspondiente. (permite cerrar: acciones Cumplida/Incumplida; el cierre automatico al cobrar es refinamiento documentado)
- [x] Permisos separados para ver cartera, gestionar, reasignar y exportar. (VER/EDITAR/EXPORTAR; reasignar via EDITAR)
- [x] Exportacion visible o filtrada respeta limites y permisos. (CSV con permiso EXPORTAR; cartera limitada a 1000 filas)
- [x] Todo cambio queda auditado. (Auditable en gestion_cobranza y promesa_pago: usuario/fecha creacion/modificacion)

## Reglas De Negocio

- La mora debe usar la misma logica que el modulo de cobros; no duplicar calculos divergentes.
- No se debe modificar el monto original de cuota desde gestion de cobranza.
- La promesa de pago no equivale a pago ni cambia estado de cuota.

## Dependencias

- Depende de: REQ-0017, REQ-0022, REQ-0052.
- Requerido por: cobranza profesional y dashboard gerencial.

## Fuentes Y Trazabilidad

- Reglas de cobro y mora: docs-migracion de negocio y modulo de cobros.
