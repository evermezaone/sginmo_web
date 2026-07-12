# REQ-0070 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | Build OK | OK |
| T02 | Deploy + smoke | 35/35 incl. dashboard-gerencial | OK |
| T03 | PrimeFaces 15 sin API de charts | usar chart.js incluido + canvas | OK (compila y renderiza) |
| T04 | JSON seguro | comillas simples, sin comillas dobles/&/</> | OK (por codigo) |

## Pruebas Manuales

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | Dashboard con datos reales | 3 graficos + resumen comparativo con colores | pendiente (verificacion visual del usuario) |
| M02 | Cambiar moneda/periodo + Actualizar | los graficos se redibujan (sin "canvas in use") | pendiente |
| M03 | KPI -> evidencia | abre dashboard-detalle filtrado | pendiente |
| M04 | Periodo sin ingresos | torta muestra "Sin ingresos" | pendiente |

## Datos De Prueba

Cobros/ingreso_egreso de varios meses para ver evolucion y composicion.

## Nota

Chart.js provisto por PrimeFaces 15 (META-INF/resources/primefaces/chart/chart.js). No se agregan dependencias.
