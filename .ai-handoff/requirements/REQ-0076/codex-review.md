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

---

## Ronda 2 - 2026-07-12

**Resultado:** REQUIERE_CAMBIOS

### Observaciones cerradas

- Obs 1 cerrada parcialmente: la autoprovision ya no inserta siempre `numero_desde=1`; intenta reactivar un rango inactivo utilizable o crear uno nuevo con `MAX(numero_hasta)+1`.

### Obs 2 - Si hay un rango activo agotado, se crea uno nuevo pero la numeracion sigue tomando el agotado

**Severidad:** alta  
**Archivo:** `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/OperacionService.java`

**Problema:** la autoprovision inserta un nuevo rango cuando no existe un rango activo utilizable (`estado='ACTIVO' AND numero_actual <= numero_hasta`). Pero no inactiva el rango activo agotado. Luego `f_siguiente_numero(:emp,'DINT','OP')` selecciona `rango_comprobante` por `tenant/tipo/serie/estado='ACTIVO' ORDER BY numero_desde FOR UPDATE`, sin filtrar `numero_actual <= numero_hasta`. Por lo tanto, si el rango viejo activo esta agotado y tiene menor `numero_desde`, la funcion lo toma primero y lanza "Timbrado agotado..." aunque el nuevo rango ya exista.

**Impacto:** el alta de operacion vuelve a fallar ante un caso normal de numerador interno agotado. El REQ busca que el alta no falle por numerador interno `DINT/OP`.

**Solucion esperada:** antes de llamar `f_siguiente_numero`, desactivar/cerrar rangos `DINT/OP` activos agotados, o ajustar `f_siguiente_numero`/provision para que la funcion seleccione un rango activo utilizable. Mantener la correccion para rangos inactivos previos y la seguridad por tenant.

## Decision ronda 2

No apruebo todavia. La colision con rango inactivo fue atendida, pero el caso de rango activo agotado sigue fallando por la forma en que `f_siguiente_numero` elige el rango.
