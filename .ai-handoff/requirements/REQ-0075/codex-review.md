# REQ-0075 - Revision Codex

**Fecha:** 2026-07-13  
**Auditor:** codex  
**Resultado:** REQUIERE_CAMBIOS

## Alcance revisado

- `.ai-handoff/requirements/REQ-0075/req.md`
- `.ai-handoff/requirements/REQ-0075/codex-review.md` anterior
- `Desarrollo/sginmo-web/src/main/resources/db/migration/V51__alertas_gerenciales.sql`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/AlertaService.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/AlertaBean.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/DashboardGerencialBean.java`
- `Desarrollo/sginmo-web/src/main/webapp/alertas.xhtml`
- `Desarrollo/sginmo-web/src/main/webapp/dashboard-gerencial.xhtml`

## Avances verificados

- El dashboard ya muestra un bloque compacto de alertas abiertas con contador y acceso a `alertas.xhtml`.
- `alertas.xhtml` ya envia `desde`, `hasta` y `ref` a `dashboard-detalle`; ya no usa `drillRef` como `hasta`.
- `contratos_por_vencer` tiene clave whitelist y detalle en `DrilldownService`.

## Observaciones

### Obs 1 - El dashboard no genera alertas automaticas, solo lista alertas existentes

**Severidad:** alta  
**Archivo:** `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/DashboardGerencialBean.java`

**Problema:** `DashboardGerencialBean.recalcular()` solo ejecuta `alertaServicio.listar()`. La generacion idempotente `AlertaService.generar()` se llama desde `AlertaBean.recalcular()`, es decir, cuando el usuario entra a la pantalla de alertas. Si nadie visito esa pantalla o recalculo alertas, el dashboard puede mostrar cero alertas aunque existan objetivos en riesgo/incumplidos.

**Impacto:** incumple el criterio "El dashboard muestra alertas automaticas cuando un objetivo esta en riesgo o incumplido". El flujo ejecutivo principal no detecta automaticamente desviaciones; depende de una visita previa a otra pantalla.

**Solucion esperada:** disparar una generacion idempotente antes de listar alertas en el dashboard, respetando permisos y sin romper la vista si falla. Alternativamente, documentar y ejecutar un job/trigger automatico real; pero para el dashboard interactivo debe mostrar alertas calculadas con datos actuales.

### Obs 2 - La evidencia de alertas por objetivo no usa el rango real del objetivo

**Severidad:** alta  
**Archivo:** `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/AlertaService.java`

**Problema:** `listar()` asigna para todas las alertas `drillDesde = inicio del mes actual` y `drillHasta = hoy`. Pero las alertas derivan de objetivos que pueden ser mensuales, trimestrales, anuales o personalizados. `REQ-0073` ya calcula `rangoDesde/rangoHasta` por objetivo, pero `AlertaService` no usa ese rango ni lo persiste en la alerta.

**Impacto:** una alerta generada por un objetivo trimestral/anual/personalizado puede abrir evidencia mensual, distinta del valor que disparo la alerta. Eso rompe la exigencia de evidencia exacta y accionable.

**Solucion esperada:** cuando se genera una alerta desde un objetivo, usar el rango real del objetivo (`getRangoDesde()`/`getRangoHasta()`) como filtros de evidencia. Si esos filtros deben sobrevivir en el tiempo, persistirlos en `alerta_gerencial` o reconstruirlos de forma deterministica desde la referencia del objetivo.

### Obs 3 - Algunos objetivos pueden generar alertas sin evidencia

**Severidad:** alta  
**Archivo:** `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/AlertaService.java`

**Problema:** `drillDeIndicador()` devuelve `null` para indicadores sin clave, por ejemplo `contratos_nuevos`. Si un objetivo activo de ese indicador queda en `ADVERTENCIA` o `CRITICO`, `generar()` inserta una alerta abierta sin evidencia (`drill_clave=null`).

**Impacto:** incumple el criterio "Cada alerta enlaza a evidencia de detalle" y la regla "Una alerta debe ser accionable; si no puede abrir evidencia o explicar causa, no debe mostrarse como critica".

**Solucion esperada:** no generar alertas accionables sin evidencia, o implementar claves whitelist para los indicadores faltantes. Para `contratos_nuevos`, puede reutilizar/crear detalle de operaciones nuevas del periodo. Si se decide permitir alertas sin evidencia, deben degradarse y explicar explicitamente la ausencia, pero eso cambiaria el criterio del REQ.

## Resultado

No apruebo `REQ-0075`. La integracion visual mejoro, pero la generacion automatica desde el dashboard y la exactitud de evidencia por alerta siguen incompletas.
