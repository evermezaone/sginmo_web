# REQ-0074 - Drill-down de evidencia para indicadores del dashboard

**Numero:** REQ-0074
**Fecha de creacion:** 2026-07-12
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

Del usuario (2026-07-12): "todo resumen debe poder llevar a la evidencia en detalle de lo que se muestra... en un click se pueda ver esas propiedades."

## Objetivo Funcional

Implementar un mecanismo comun de evidencia para que cada KPI, grafico, subtotal u objetivo del dashboard
pueda abrir el detalle exacto que compone el numero mostrado.

## Criterios De Aceptacion

- [x] Existe pantalla `dashboard-detalle.xhtml` para mostrar evidencia filtrada por indicador. (creada; tabla dinamica + estado vacio)
- [x] Cada KPI expone una clave de detalle y parametros firmes: periodo, moneda, sucursal, tipo, estado, ref. (view params clave/desde/hasta/moneda/sucursal/ref; los KPIs de 0069/0071/0072/0073 ya exponen drillKey)
- [x] El detalle de mora abre cuotas/clientes que componen el monto. (clave `mora`: cronograma_cuota vencidas + cliente + moneda + saldo)
- [x] El detalle de cobros abre cobros reales del periodo. (clave `cobros`)
- [x] El detalle de ingresos/egresos abre movimientos que componen cada subtotal. (claves `ingresos`/`egresos`: ingreso_egreso por aplicacion)
- [x] El detalle de ocupacion/vacancia abre propiedades que componen ocupado/vacante. (claves `ocupacion`/`vacancia`)
- [x] El detalle de rentabilidad abre ingresos/egresos del activo. (clave `rentabilidad_activo` con ref=activo)
- [x] La pantalla permite exportar CSV con filtros aplicados y sin mezclar monedas. (export CSV con encabezado de filtros; el filtro de moneda evita mezcla)
- [x] Los enlaces de drill-down no aceptan rutas/query libres (anti-injection): whitelist de indicadores y filtros. (DrilldownService.PERMISO = whitelist; consultas parametrizadas; parametros tipados -LocalDate/Long-)
- [x] El detalle respeta permisos del modulo origen (ver dashboard no basta). (cada clave exige su permiso: cobros->caja/VER, mora->cobranza/VER, ing/egr->ingresos-egresos/VER, ocup/vac->ocupacion/VER, rent->rentabilidad/VER; ademas dashboard-gerencial/VER para abrir)

## Reglas De Negocio

- Si un resumen dice "10 propiedades faltantes", el detalle debe contener esas mismas 10 o explicar por que la lista cambio por datos actualizados.
- Cada detalle debe mostrar fecha/hora de generacion y filtros aplicados.
- El usuario debe poder volver al dashboard conservando filtros.

## Dependencias

- Depende de: REQ-0062, REQ-0069, REQ-0071, REQ-0072, REQ-0073.
- Requerido por: todos los dashboards gerenciales.

## Fuentes Y Trazabilidad

- Pedido directo del usuario sobre evidencia clicable de los resumenes.
- Estandar ABM: filtros/orden por whitelist.
