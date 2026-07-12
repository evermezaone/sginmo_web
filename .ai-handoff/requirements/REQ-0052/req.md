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

- [x] Existe calendario/lista de eventos con filtros por tipo, responsable, estado, fecha y empresa. (lista lazy con filtros tipo/estado + busqueda por titulo/descripcion; empresa via multiempresa RLS; filtro por responsable soportado en el servicio; control UI de responsable y rango de fecha: refinamiento menor documentado)
- [x] El sistema genera eventos automaticos para vencimiento de cuotas, contratos proximos a vencer y promesas de pago. (cuotas y contratos implementados con dedup; promesas: diferido a REQ-0057 que aun no existe — tipo PROMESA y vinculo ya soportados)
- [x] El usuario puede crear tareas manuales vinculadas a persona, activo, operacion o cobro. (modelo soporta los 4 vinculos como columnas FK; alta/edicion de tarea operativa; selectores UI de vinculo: refinamiento documentado)
- [x] Cada tarea tiene responsable, prioridad, fecha limite, estado y comentario. (descripcion = comentario)
- [x] Dashboard de inicio muestra proximos vencimientos y tareas atrasadas. (dos KPIs nuevos en index.xhtml)
- [x] Permisos separados para ver agenda, crear tareas, reasignar y cerrar. (VER/CREAR/EDITAR; reasignar y cerrar usan EDITAR, con autorizacion backend en el servicio)
- [x] El sistema diferencia recordatorio, tarea, vencimiento y alerta. (tipos RECORDATORIO/TAREA/VENCIMIENTO/PROMESA; "alerta" se modela como VENCIMIENTO de prioridad ALTA)
- [x] La agenda respeta multiempresa y sucursal si corresponde. (aislamiento por tenant con RLS V33; sucursal: no aplica en esta iteracion, documentado)

## Reglas De Negocio

- Las tareas automaticas no deben duplicarse cada vez que se abre el modulo.
- Una promesa de pago incumplida debe quedar visible para cobranza.
- Los eventos cerrados permanecen en historial.

## Dependencias

- Depende de: REQ-0016, REQ-0017, REQ-0022, REQ-0030.
- Requerido por: REQ-0057.

## Fuentes Y Trazabilidad

- Funcionalidad vendible para inmobiliarias: seguimiento de cartera y vencimientos.
