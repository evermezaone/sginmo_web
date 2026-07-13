# REQ-0076 - Revision Codex

**Fecha:** 2026-07-12  
**Auditor:** codex  
**Resultado:** REQUIERE_CAMBIOS

## Alcance revisado

- `.ai-handoff/requirements/REQ-0076/req.md`
- `.ai-handoff/requirements/REQ-0076/claude-implementation.md`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/OperacionService.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/OperacionBean.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/one/core/ErroresBd.java`
- `Desarrollo/sginmo-web/src/main/webapp/operaciones.xhtml`
- Migraciones `V1`, `V26`, `V27` para `rango_comprobante`, `documento` y `f_siguiente_numero`.

## Observaciones

### Obs 1 - La autoprovision DINT/OP falla si existe un rango no activo previo

**Severidad:** alta  
**Archivo:** `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/OperacionService.java`

**Problema:** `crearDocumentoInterno()` autoprovisiona siempre un rango `DINT/OP` con `numero_desde = 1` cuando no existe un rango `ACTIVO`. Pero el esquema original mantiene unicidad por empresa/tipo/serie/numero_desde (`V1`) y luego `V26` renombra esas columnas a tenant/tipo/serie. Si ya existe un rango `DINT/OP` inactivo, vencido o agotado con `numero_desde = 1`, el `NOT EXISTS` por `estado = 'ACTIVO'` permite intentar el insert, pero la unicidad rechaza la fila antes de llegar a `f_siguiente_numero()`.

**Impacto:** el alta de operacion puede volver a fallar justamente en el caso "no hay timbrado/rango activo", solo que ahora con una violacion de clave unica. El REQ pide que el alta ya no falle por falta de `DINT/OP` activo.

**Solucion esperada:** hacer la provision robusta ante rangos previos: reactivar/reabrir el rango interno existente si corresponde, o crear un rango nuevo con `numero_desde` calculado desde el maximo existente, o agregar una estrategia `ON CONFLICT` coherente con la restriccion. Debe seguir siendo tenant-safe e idempotente.

## Validaciones que si cumplen

- `ErroresBd.traducir()` reconoce `SQLState P0001` y devuelve `NegocioException` con mensaje limpio de una linea.
- `OperacionBean.crear()` captura `NegocioException` y `RuntimeException`.
- El boton `Registrar operacion` actualiza `msjEdicion`, `frmLista:tabla` y `frmLista:mensajes`, por lo que el dialogo tiene un canal visible para los mensajes.
- `f_siguiente_numero()` conserva `FOR UPDATE` para numeracion concurrente sobre el rango seleccionado.
