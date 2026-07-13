# REQ-0070 - Auditoria Codex

Fecha: 2026-07-12
Auditor: codex
Decision: REQUIERE_CAMBIOS

## Alcance revisado

- `.ai-handoff/requirements/REQ-0070/req.md`
- `.ai-handoff/requirements/REQ-0070/claude-implementation.md`
- `.ai-handoff/requirements/REQ-0070/preaudit-checklist.md`
- `Desarrollo/sginmo-web/src/main/webapp/dashboard-gerencial.xhtml`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/DashboardGerencialBean.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/DashboardMetricasService.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/DrilldownService.java`

## Verificacion

- `mvn -q clean package` en `Desarrollo`: OK.
- Inspeccion de codigo real: completada.

## Hallazgos

### Obs 1 - Los graficos de evolucion no respetan el periodo filtrado

Problema: `recalcular()` toma `desde/hasta` para KPIs y torta de distribucion, pero los graficos de linea y barras usan `metricas.serieMensual(..., 12)`, y esa serie siempre calcula los ultimos 12 meses hasta `LocalDate.now()`. Cambiar el filtro `hasta` o el periodo visible no mueve la ventana temporal de esos graficos.

Impacto: incumple el criterio "Los graficos respetan los filtros de periodo, moneda (sucursal cuando aplique)". El usuario puede seleccionar un periodo y ver KPIs/distribucion de ese periodo, pero evolucion/barras de otro rango temporal, generando lectura gerencial inconsistente.

Solucion esperada: hacer que la serie mensual reciba al menos la fecha de referencia `hasta` o el rango seleccionado, y que los graficos se recalculen con la misma ventana/filtros que la pantalla declara. Si se decide que "ultimos 12 meses" es siempre relativo a hoy, la UI no debe presentarlo como dependiente del filtro de periodo.

Evidencia:
- `DashboardGerencialBean.java:71-80`
- `DashboardMetricasService.java:124-138`

### Obs 2 - No cada KPI enlaza a evidencia filtrada

Problema: el resumen ejecutivo comparativo renderiza los 9 KPIs como `<div class="kpi-g">` sin link ni parametros de evidencia. Solo dos KPIs operativos posteriores tienen `h:link` a `dashboard-detalle`: mora y cobros.

Impacto: incumple el criterio "Cada KPI enlaza a evidencia filtrada (REQ-0074)". Los KPIs comparativos principales como ingresos, egresos, rentabilidad, ocupacion, vacancia y contratos no tienen drill-down, pese a que el REQ pide que el resumen sea navegable a evidencia.

Solucion esperada: convertir los KPIs comparativos aplicables en enlaces a `dashboard-detalle` usando `drillKey`, periodo, moneda y sucursal cuando corresponda; si una clave aun no existe en REQ-0074, el KPI debe indicar claramente que la evidencia esta pendiente o no prometer ese criterio como cerrado.

Evidencia:
- `dashboard-gerencial.xhtml:37-53`
- `dashboard-gerencial.xhtml:90-110`

### Obs 3 - Falta la tabla/enlace de ocupacion-vacancia exigida

Problema: el REQ exige "Incluye tabla de ocupacion/vacancia (pantalla Ocupacion, REQ-0072) enlazada". La pantalla muestra KPIs comparativos y tarjetas operativas, pero no incluye una tabla/breakdown de ocupacion-vacancia ni un enlace visible al modulo ocupacion desde esa seccion.

Impacto: el dashboard queda incompleto frente al pedido del usuario de llevar cada resumen a evidencia y ver ocupacion/vacancia de forma ejecutiva. Ademas, ocupacion/vacancia aparecen solo como tarjetas comparativas no navegables.

Solucion esperada: agregar una seccion compacta de ocupacion/vacancia con enlace al modulo `ocupacion.xhtml` y/o al drill-down `dashboard-detalle` para ocupacion/vacancia, manteniendo responsive y sin tarjetas anidadas.

Evidencia:
- `dashboard-gerencial.xhtml:37-111`

## Resultado

No apruebo REQ-0070. La base visual y Chart.js compilan, pero faltan criterios funcionales centrales: filtros coherentes en graficos, evidencia por KPI y ocupacion/vacancia enlazada.

---

## Ronda 2 - 2026-07-12

**Resultado:** REQUIERE_CAMBIOS

### Observaciones cerradas

- Obs 1 cerrada: `DashboardGerencialBean.construirGraficos()` ahora llama `serieMensual(..., hasta)` y `DashboardMetricasService.serieMensual()` arma la ventana de 12 meses terminando en el mes del filtro.
- Obs 2 cerrada: los comparativos navegables exponen `detalleClave` y la vista agrega `h:link` a `dashboard-detalle` para KPIs con evidencia.
- Obs 3 cerrada: la vista agrega seccion `Ocupacion y vacancia` con enlace al modulo `ocupacion` y a la evidencia `vacancia`.

### Obs 4 - El dashboard calcula ocupacion/vacancia sin exigir operacion vigente

**Severidad:** alta  
**Archivo:** `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/DashboardMetricasService.java`

**Problema:** el dashboard duplica la logica de ocupacion/vacancia en `DashboardMetricasService.ocupados()`, contando operaciones `ALQUILER` por fechas (`fecha_inicio_contrato` / `fecha_finalizacion`) pero sin exigir `o.estado = 'VIGENTE'`.

**Impacto:** el tablero gerencial puede mostrar ocupacion, vacancia y brechas con operaciones finalizadas o anuladas si sus fechas quedan inconsistentes. Es el mismo defecto detectado en `REQ-0072`, pero aqui afecta el resumen ejecutivo y la seccion del dashboard.

**Solucion esperada:** alinear el calculo con la regla de operacion vigente: agregar `o.estado='VIGENTE'` o reutilizar un unico servicio/regla compartida con `OcupacionService` para evitar divergencias entre pantalla de ocupacion y dashboard.

## Decision ronda 2

No apruebo todavia. Las tres observaciones originales fueron corregidas, pero la ocupacion/vacancia del dashboard queda funcionalmente inconsistente con el criterio de activo ocupado por alquiler vigente.

---

## Ronda 3 - 2026-07-12

**Resultado:** APROBADO_POR_CODEX

### Observaciones cerradas

- Obs 4 cerrada: `DashboardMetricasService.ocupados()` ahora filtra `o.estado='VIGENTE'` junto con `tipo_operacion='ALQUILER'` y las fechas de vigencia.

### Verificacion ronda 3

- Inspeccion de codigo real: completada.
- `mvn -q clean package` en `Desarrollo`: OK en la verificacion ejecutada durante esta tanda.

## Decision ronda 3

Apruebo `REQ-0070`. Los graficos respetan el periodo de corte, los KPIs navegables enlazan a evidencia, la seccion ocupacion/vacancia existe, y el calculo de ocupacion ya exige alquiler vigente.
