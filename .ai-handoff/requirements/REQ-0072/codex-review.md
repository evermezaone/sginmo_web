# REQ-0072 - Revision Codex

**Fecha:** 2026-07-12  
**Auditor:** codex  
**Resultado:** REQUIERE_CAMBIOS

## Alcance revisado

- `.ai-handoff/requirements/REQ-0072/req.md`
- `.ai-handoff/requirements/REQ-0072/claude-implementation.md`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/OcupacionService.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/OcupacionBean.java`
- `Desarrollo/sginmo-web/src/main/webapp/ocupacion.xhtml`
- Migraciones de esquema para confirmar `activo.tipo`.

## Observaciones

### Obs 1 - La ocupacion no exige operacion vigente

**Severidad:** alta  
**Archivo:** `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/OcupacionService.java`

**Problema:** la subconsulta `OCUPADOS_SUB` considera ocupado a todo activo con operacion `ALQUILER` que cubre la fecha por `fecha_inicio_contrato` / `fecha_finalizacion`, pero no filtra `o.estado = 'VIGENTE'`.

**Impacto:** una operacion finalizada o anulada con fechas inconsistentes puede seguir contando como ocupacion. Eso distorsiona ocupacion, vacancia y brecha contra objetivo, que son indicadores gerenciales.

**Solucion esperada:** agregar el filtro de estado vigente en la subconsulta central de ocupados y reutilizarlo en `resumen()`, `vacantes()` y `porTipo()`. Si hay mas estados validos, documentarlos y parametrizarlos explicitamente.

### Obs 2 - Las propiedades vacantes no se pueden abrir en un click

**Severidad:** alta  
**Archivo:** `Desarrollo/sginmo-web/src/main/webapp/ocupacion.xhtml`

**Problema:** el criterio pide abrir en un click las propiedades vacantes que faltan para cumplir el objetivo. La pantalla lista las vacantes y marca las primeras dentro de la brecha, pero la columna `Propiedad` renderiza solo texto (`h:outputText`) y no hay enlace/accion hacia la ficha o ABM del activo.

**Impacto:** el usuario ve la evidencia agregada pero no puede navegar desde el indicador a la evidencia operativa accionable, que fue parte explicita del pedido.

**Solucion esperada:** hacer clicable cada propiedad vacante hacia la ficha/edicion/consulta del activo, respetando permisos. Si el ABM de activos no tiene modo detalle por id, agregar una accion segura en el flujo existente.

## Notas

- La sospecha de columna invalida `activo.tipo` fue descartada: `V26__multiempresa_esquema.sql` migra `tipo_lista/tipo_codigo` a `activo.tipo` FK a `entidad`.
- El build declarado por Claude no detectaria estas dos observaciones porque son de regla funcional y navegacion.
