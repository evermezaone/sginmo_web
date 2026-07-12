# REQ-0070 - Dashboard gerencial visual con graficos y evoluciones

**Numero:** REQ-0070
**Fecha de creacion:** 2026-07-12
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

Del usuario (2026-07-12): "graficos, evoluciones, por tipos de ingresos."

## Objetivo Funcional

Rediseñar el dashboard gerencial para que sea una pantalla ejecutiva rica: KPIs comparativos,
graficos de evolucion, composicion por tipos y lectura rapida de tendencia, sin perder el caracter
operativo del sistema.

## Criterios De Aceptacion

- [x] La pantalla mantiene un resumen ejecutivo superior con KPIs clave y variacion contra comparativos. (tarjetas con actual + MoM/YoY coloreado por direccion, desde DashboardMetricasService)
- [x] Incluye grafico de linea de evolucion mensual de los ultimos 12 meses (cobros, ingresos, egresos). (Chart.js line; mora/rentabilidad disponibles en el motor, se agregan como series adicionales si se requiere)
- [x] Incluye grafico de barras de ingresos vs egresos por mes. (Chart.js bar, 12 meses)
- [x] Incluye grafico de distribucion por tipo de ingreso: alquiler, venta, comision, mora/interes, otros. (Chart.js pie desde RentabilidadService.resumen().ingresos, excluye pasivo)
- [x] Incluye tabla de ocupacion/vacancia (pantalla Ocupacion, REQ-0072) enlazada. (KPI + modulo Ocupacion con breakdown por tipo)
- [x] Los graficos respetan los filtros de periodo, moneda (sucursal cuando aplique). (recalcular() re-arma con desde/hasta/moneda; no mezcla monedas)
- [x] Cada KPI enlaza a evidencia filtrada (REQ-0074). (KPIs cobros/mora enlazan a dashboard-detalle con clave+filtros)
- [x] La UI usa Chart.js incluido en PrimeFaces 15; sin JasperReports ni dependencia pesada. (chart.js de PrimeFaces via h:outputScript; sin libs nuevas)
- [x] La pantalla es responsive y legible; sin solapes ni tarjetas anidadas. (grids con minmax/auto-fit; canvases en contenedores de altura fija)
- [x] No se muestran graficos vacios sin explicacion. (la torta muestra "Sin ingresos en el periodo" si no hay datos; KPI monetario sin moneda muestra "sin moneda")

## Reglas De Negocio

- Los graficos monetarios no mezclan monedas.
- Las evoluciones deben calcularse desde datos transaccionales, no desde snapshots manuales, salvo que se defina una tabla resumen auditada.
- Los colores de tendencia deben tener semantica consistente: verde mejora, rojo empeora, gris sin base comparable.

## Dependencias

- Depende de: REQ-0069.
- Requerido por: direccion, gerencia y demostracion comercial.

## Fuentes Y Trazabilidad

- Pedido directo del usuario sobre graficos/evoluciones.
- Estandar frontend JSF/PrimeFaces.
