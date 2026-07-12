# REQ-0070 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0070
- Tipo de cambio: UI (dashboard-gerencial.xhtml) + backend liviano (DashboardGerencialBean + serieMensual en el motor). Sin BD.
- Riesgo: bajo (solo lectura/UI)
- Archivos clave:
  - `web/DashboardGerencialBean.java`: agrega comparativos (DashboardMetricasService) + construye JSON (comillas simples) para Chart.js (evolucion 12m cobros/ingresos/egresos, barras ing vs egr, torta de composicion desde RentabilidadService).
  - `servicio/DashboardMetricasService.java`: + serieMensual(indicador, moneda, sucursal, meses) + Punto.
  - `webapp/dashboard-gerencial.xhtml`: resumen ejecutivo comparativo (MoM/YoY con color por direccion) + 3 graficos Chart.js (chart.js de PrimeFaces via h:outputScript) + KPIs operativos con enlace a evidencia (REQ-0074).
  - `tools/smoke-test-vps.py`: ya cubierto (dashboard-gerencial).
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - Deploy + `python tools/smoke-test-vps.py`: 35/35 incl. `dashboard-gerencial`.
- Cambios de datos: no. Cambios de entorno: no.
- Decision esperada: aprobar; revisar la eleccion de Chart.js directo (PrimeFaces 15 removio la API Java de charts) y el escapado del JSON.
- Notas para auditor:
  - PrimeFaces 15 NO trae `org.primefaces.model.charts.*`; se usa el chart.js que PrimeFaces incluye (h:outputScript library=primefaces name=chart/chart.js) y canvases.
  - JSON con comillas simples y sin `<`/`>`/comillas dobles (seguro en texto HTML); numeros con toPlainString; el script se re-ejecuta en ajax y destruye los charts previos (evita "canvas in use").
  - No mezcla monedas: series monetarias usan la moneda seleccionada.
  - Cada KPI enlaza a dashboard-detalle con clave+filtros (permiso por modulo, REQ-0074).

## Resumen Funcional

El dashboard pasa de tarjetas simples a un tablero ejecutivo: resumen comparativo (mes vs mes, vs anio
anterior) con colores de tendencia, evolucion mensual, ingresos vs egresos y composicion de ingresos,
con KPIs que abren su evidencia.

## Resumen Tecnico

Bean expone comparativos + JSON para Chart.js; la vista dibuja 3 graficos y enlaza a la evidencia.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| web/DashboardGerencialBean.java | comparativos + JSON Chart.js |
| servicio/DashboardMetricasService.java | + serieMensual + Punto |
| webapp/dashboard-gerencial.xhtml | resumen comparativo + 3 graficos + enlaces a evidencia |

## Cambios De Datos

Sin cambios.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; deploy; smoke 35/35 (dashboard-gerencial 200).

## Pruebas Manuales Sugeridas

1. Abrir el dashboard y ver los 3 graficos con datos reales; cambiar moneda/periodo y Actualizar.
2. Click en "Cuotas vencidas" / "Cobrado" -> abre la evidencia filtrada (REQ-0074).
3. Periodo sin ingresos -> la torta muestra "Sin ingresos en el periodo".

## Limitaciones Conocidas

- Series de mora/rentabilidad y breakdown de ocupacion por zona/sucursal: incremental (motor ya las provee).

## Riesgos Conocidos

- UI/solo lectura; riesgo bajo.
