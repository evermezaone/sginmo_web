# REQ-0074 - Revision Codex

**Fecha:** 2026-07-13  
**Auditor:** codex  
**Resultado:** APROBADO

## Alcance revisado

- `.ai-handoff/requirements/REQ-0074/req.md`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/DrilldownService.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/DashboardDetalleBean.java`
- `Desarrollo/sginmo-web/src/main/webapp/dashboard-detalle.xhtml`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/DashboardGerencialBean.java`
- `Desarrollo/sginmo-web/src/main/webapp/dashboard-gerencial.xhtml`

## Re-auditoria

Las observaciones previas quedaron cerradas:

- `DrilldownService.propiedades()` ahora filtra la subconsulta de ocupacion/vacancia con `o.tipo_operacion='ALQUILER' AND o.estado='VIGENTE'` y fechas, alineado con la regla del KPI.
- `dashboard-detalle.xhtml` reenvia filtros al volver al dashboard (`fdesde`, `fhasta`, `fmoneda`, `fsucursal`).
- `dashboard-gerencial.xhtml` consume esos view params y `DashboardGerencialBean.aplicarParametros()` recalcula despues de aplicarlos.

Se mantienen controles relevantes del REQ:

- Whitelist fija de claves en `DrilldownService.PERMISO`.
- Consultas parametrizadas, sin rutas/query libres.
- Permiso del modulo origen por clave, ademas del acceso al dashboard.
- CSV con titulo, filtros y fecha de generacion.

## Verificacion

- `mvn -q -pl sginmo-web -am clean package`: EXIT 0.

## Resultado

Apruebo `REQ-0074`. No quedan hallazgos bloqueantes en el alcance auditado.
