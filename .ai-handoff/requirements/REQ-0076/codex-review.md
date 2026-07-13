# REQ-0076 - Revision Codex

**Fecha:** 2026-07-13  
**Auditor:** codex  
**Resultado:** APROBADO

## Alcance revisado

- `.ai-handoff/requirements/REQ-0076/req.md`
- `.ai-handoff/requirements/REQ-0076/claude-implementation.md`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/OperacionService.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/OperacionBean.java`
- `Desarrollo/onesystem-security/src/main/java/py/com/one/core/ErroresBd.java`
- `Desarrollo/sginmo-web/src/main/webapp/operaciones.xhtml`
- Migraciones `V1`, `V26`, `V27` para `rango_comprobante`, `documento` y `f_siguiente_numero`.

## Resultado final

APROBADO. Las observaciones previas quedaron corregidas.

## Validaciones realizadas

- `crearDocumentoInterno()` desactiva rangos `DINT/OP` activos agotados antes de llamar a `f_siguiente_numero()`, evitando que la funcion tome primero un rango viejo agotado.
- Si no existe rango activo utilizable, reactiva un rango interno inactivo con saldo disponible.
- Si tampoco existe rango reutilizable, crea un nuevo rango `DINT/OP` con `numero_desde = MAX(numero_hasta) + 1`, evitando la colision historica con `numero_desde = 1`.
- La provision se mantiene acotada por tenant, tipo `DINT` y serie `OP`.
- `ErroresBd.traducir()` reconoce `SQLState P0001` y devuelve `NegocioException` con mensaje limpio.
- `OperacionBean.crear()` muestra `NegocioException` como advertencia de negocio y `RuntimeException` como error visible.
- El boton `Registrar operacion` actualiza `msjEdicion`, `frmLista:tabla` y `frmLista:mensajes`, por lo que los errores no quedan silenciosos en el dialogo.

## Verificacion

```text
mvn -q -pl sginmo-web -am clean package
EXIT 0
```

## Historial de observaciones cerradas

- Obs 1 cerrada: la provision ya no intenta insertar siempre `numero_desde = 1` cuando existe un rango interno previo no activo.
- Obs 2 cerrada: los rangos activos agotados se inactivan antes de numerar, por lo que `f_siguiente_numero()` no queda atrapada en el rango viejo.
