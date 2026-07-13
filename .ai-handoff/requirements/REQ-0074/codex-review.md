# REQ-0074 - Revision Codex

**Fecha:** 2026-07-12  
**Auditor:** codex  
**Resultado:** REQUIERE_CAMBIOS

## Alcance revisado

- `.ai-handoff/requirements/REQ-0074/req.md`
- `.ai-handoff/requirements/REQ-0074/claude-implementation.md`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/DrilldownService.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/DashboardDetalleBean.java`
- `Desarrollo/sginmo-web/src/main/webapp/dashboard-detalle.xhtml`

## Observaciones

### Obs 1 - El detalle de ocupacion/vacancia no exige operacion vigente

**Severidad:** alta  
**Archivo:** `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/DrilldownService.java`

**Problema:** `propiedades()` arma la evidencia de `ocupacion`/`vacancia` con subconsulta de operaciones `ALQUILER` por fechas, pero no filtra `o.estado='VIGENTE'`.

**Impacto:** el detalle puede mostrar propiedades ocupadas/vacantes distinto al KPI y a la pantalla `ocupacion`, que ya fueron corregidos para exigir alquiler vigente. La evidencia deja de explicar exactamente el numero mostrado.

**Solucion esperada:** alinear la subconsulta con la regla central de ocupacion: `tipo_operacion='ALQUILER' AND estado='VIGENTE'` mas fechas. Idealmente reutilizar una constante/servicio comun para no volver a divergir.

### Obs 2 - Volver al dashboard no conserva filtros

**Severidad:** media  
**Archivo:** `Desarrollo/sginmo-web/src/main/webapp/dashboard-detalle.xhtml`

**Problema:** el REQ exige poder volver al dashboard conservando filtros. El boton `Volver al dashboard` navega a `dashboard-gerencial` sin reenviar `desde`, `hasta`, `moneda` ni `sucursal`.

**Impacto:** el usuario abre evidencia filtrada, vuelve, y pierde el contexto que explicaba ese detalle.

**Solucion esperada:** reenviar los filtros relevantes como parametros o mantenerlos en estado de vista/preferencia. Si el dashboard aun no consume view params, agregar soporte coherente.

## Validaciones que si cumplen

- La entrada de detalle usa `clave` por whitelist fija.
- Las consultas usan parametros tipados para filtros variables.
- El servicio exige permiso del modulo origen por clave, no solo permiso de dashboard.
- El CSV incluye titulo, filtros y fecha de generacion.

## Resultado

No apruebo `REQ-0074` hasta alinear ocupacion/vacancia con alquiler vigente y preservar los filtros al volver.
