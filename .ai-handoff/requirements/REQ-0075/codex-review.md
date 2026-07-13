# REQ-0075 - Revision Codex

**Fecha:** 2026-07-12  
**Auditor:** codex  
**Resultado:** REQUIERE_CAMBIOS

## Alcance revisado

- `.ai-handoff/requirements/REQ-0075/req.md`
- `.ai-handoff/requirements/REQ-0075/claude-implementation.md`
- `Desarrollo/sginmo-web/src/main/resources/db/migration/V51__alertas_gerenciales.sql`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/AlertaService.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/AlertaBean.java`
- `Desarrollo/sginmo-web/src/main/webapp/alertas.xhtml`
- `Desarrollo/sginmo-web/src/main/webapp/dashboard-gerencial.xhtml`

## Observaciones

### Obs 1 - El dashboard no muestra alertas automaticas

**Severidad:** alta  
**Archivo:** `Desarrollo/sginmo-web/src/main/webapp/dashboard-gerencial.xhtml`

**Problema:** el criterio dice que el dashboard muestra alertas automaticas cuando un objetivo esta en riesgo o incumplido. La implementacion agrega una pantalla `alertas.xhtml`, pero `dashboard-gerencial.xhtml` no muestra alertas, contador, resumen, ni enlace contextual a las alertas abiertas.

**Impacto:** el usuario puede abrir el dashboard y no enterarse de desviaciones criticas. La alerta queda fuera del flujo ejecutivo principal.

**Solucion esperada:** integrar en el dashboard un bloque compacto de alertas abiertas/prioritarias o al menos un resumen con contador por prioridad y acceso directo a `alertas.xhtml`, respetando permisos.

### Obs 2 - El enlace de evidencia pasa parametros incorrectos

**Severidad:** alta  
**Archivo:** `Desarrollo/sginmo-web/src/main/webapp/alertas.xhtml`

**Problema:** el enlace a `dashboard-detalle` envia `clave=#{a.drillClave}` pero pasa `hasta=#{a.drillRef}`. `drillRef` es una referencia numerica, no una fecha de corte. Ademas, la mayoria de alertas se crean con `drillRef=null`, por lo que el detalle se abre sin periodo/fecha.

**Impacto:** la evidencia puede cargar vacia, con filtros erroneos o con fecha nula. La alerta deja de ser accionable y no explica la causa/impacto mostrado.

**Solucion esperada:** guardar y pasar los filtros reales de la alerta: periodo desde/hasta, moneda/sucursal/ref segun corresponda. Si `drill_ref` representa una entidad, enviarlo como `ref`, no como `hasta`.

### Obs 3 - Hay alertas sin evidencia aunque el REQ exige evidencia por alerta

**Severidad:** alta  
**Archivo:** `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/AlertaService.java`

**Problema:** `contratos_por_vencer` se inserta con `drill_clave=null`, y otros indicadores sin clave en `drillDeIndicador()` tampoco tienen evidencia. El REQ exige que cada alerta enlace a evidencia de detalle.

**Impacto:** se muestran alertas accionables sin poder abrir el conjunto de registros que las explica. En contratos por vencer, el usuario no puede ver cuales contratos vencen.

**Solucion esperada:** agregar clave whitelist y detalle para contratos por vencer, o no mostrar la alerta como accionable hasta tener evidencia. Para indicadores sin evidencia, definir clave o degradar la prioridad/explicar la ausencia segun regla de negocio.

## Notas

- La tabla, RLS, deduplicacion parcial por alerta abierta y cierre con motivo estan encaminados.
- `REQ-0075` tambien hereda los bloqueos de `REQ-0073` y `REQ-0074`: objetivos y evidencia aun tienen observaciones abiertas.

## Resultado

No apruebo `REQ-0075`. Faltan integracion con dashboard y evidencia fiable por alerta.
