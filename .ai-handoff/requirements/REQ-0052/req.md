# REQ-0052 - Agenda, recordatorios y vencimientos operativos

**Numero:** REQ-0052
**Fecha de creacion:** 2026-07-11
**Estado inicial:** NUEVO
**Prioridad:** media

## Texto Original

Proponer funcionalidades utiles, vendibles y atractivas para el sistema.

## Objetivo Funcional

Agregar agenda operativa para administrar vencimientos de cuotas, contratos, promesas de pago, tareas internas y seguimientos de clientes/propiedades.

## Criterios De Aceptacion

- [ ] Existe calendario/lista de eventos con filtros por tipo, responsable, estado, fecha y empresa.
- [ ] El sistema genera eventos automaticos para vencimiento de cuotas, contratos proximos a vencer y promesas de pago.
- [ ] El usuario puede crear tareas manuales vinculadas a persona, activo, operacion o cobro.
- [ ] Cada tarea tiene responsable, prioridad, fecha limite, estado y comentario.
- [ ] Dashboard de inicio muestra proximos vencimientos y tareas atrasadas.
- [ ] Permisos separados para ver agenda, crear tareas, reasignar y cerrar.
- [ ] El sistema diferencia recordatorio, tarea, vencimiento y alerta.
- [ ] La agenda respeta multiempresa y sucursal si corresponde.

## Reglas De Negocio

- Las tareas automaticas no deben duplicarse cada vez que se abre el modulo.
- Una promesa de pago incumplida debe quedar visible para cobranza.
- Los eventos cerrados permanecen en historial.

## Dependencias

- Depende de: REQ-0016, REQ-0017, REQ-0022, REQ-0030.
- Requerido por: REQ-0057.

## Fuentes Y Trazabilidad

- Funcionalidad vendible para inmobiliarias: seguimiento de cartera y vencimientos.
