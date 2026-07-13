# REQ-0075 - Revision Codex

**Fecha:** 2026-07-13  
**Auditor:** codex  
**Resultado:** APROBADO

## Alcance revisado

- `.ai-handoff/requirements/REQ-0075/req.md`
- `Desarrollo/sginmo-web/src/main/resources/db/migration/V51__alertas_gerenciales.sql`
- `Desarrollo/sginmo-web/src/main/resources/db/migration/V53__alerta_rango_evidencia.sql`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/AlertaService.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/DrilldownService.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/DashboardGerencialBean.java`
- `Desarrollo/sginmo-web/src/main/webapp/alertas.xhtml`
- `Desarrollo/sginmo-web/src/main/webapp/dashboard-gerencial.xhtml`

## Re-auditoria

Las observaciones previas quedaron cerradas:

- `DashboardGerencialBean.recalcular()` genera alertas idempotentemente antes de listarlas, por lo que el dashboard ya no depende de que alguien visite `alertas.xhtml`.
- `V53__alerta_rango_evidencia.sql` agrega `drill_desde` y `drill_hasta`; `AlertaService` persiste el rango real del objetivo y lo usa al listar.
- Las alertas por objetivo sin evidencia ya no se generan; `contratos_nuevos` tiene clave propia y detalle whitelist en `DrilldownService`.
- `contratos_por_vencer` mantiene evidencia con clave whitelist.

Controles revisados:

- Deduplicacion por `hash_dedup` mientras la alerta esta abierta.
- Cierre `DESCARTADA` con motivo obligatorio.
- Permisos `alertas/VER` y `alertas/EDITAR`.
- RLS en tabla `alerta_gerencial`.

## Verificacion

- `mvn -q -pl sginmo-web -am clean package`: EXIT 0.

## Resultado

Apruebo `REQ-0075`. No quedan hallazgos bloqueantes en el alcance auditado.
