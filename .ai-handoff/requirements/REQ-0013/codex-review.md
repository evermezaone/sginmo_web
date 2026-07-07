# Codex Review - REQ-0013

Fecha: 2026-07-07
Auditor: codex
Resultado: REQUIERE_CAMBIOS

## Verificacion

- Leidos `req.md`, `claude-implementation.md`, `test-plan.md` y `preaudit-checklist.md`.
- Revisados:
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/dominio/activo/Activo.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/dominio/activo/ActivoPropietario.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/dominio/activo/ActivoAtributoValor.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/ActivoService.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/ActivoBean.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/ActivoConverter.java`
  - `Desarrollo/sginmo-web/src/main/webapp/activos.xhtml`
  - `Desarrollo/sginmo-web/src/main/resources/db/migration/V1__esquema_inicial.sql`
  - `Desarrollo/sginmo-web/src/main/resources/db/migration/V15__pantalla_activos.sql`

Confirmado correcto:

- El ABM usa `LazyDataModel`, busqueda global, orden con whitelist y modo solo lectura.
- El servicio tiene enforcement backend para `CREAR`/`EDITAR`.
- El activo recursivo usa `padre` como contenedor y la UI ofrece autocomplete.
- Los propietarios posibles salen de `PersonaService.porRol("PROPIETARIO")`, que filtra personas/roles activos.
- Los atributos por tipo se cargan desde `atributo_por_tipo` y se valida obligatoriedad al guardar.
- El estado operativo del activo queda fuera del ABM, como corresponde para operaciones futuras.

## Hallazgos Bloqueantes

### Obs 212 - Propietarios/atributos se escriben con SQL nativo y pierden auditoria real; propietarios se borran fisicamente

Problema: `ActivoService.agregarPropietario()` inserta en `activo_propietario` con SQL nativo y `usuario_creacion='sistema'`; `quitarPropietario()` ejecuta `DELETE FROM activo_propietario`. Lo mismo ocurre en `guardarValorAtributo()` para `activo_atributo`: INSERT/UPDATE/DELETE nativos con usuario fijo `sistema`. Ademas `ActivoBean.quitarPropietario()` no captura `NegocioException`.

Impacto: en un sistema inmobiliario, propietario de activo y atributos catastrales/tipificados son datos sensibles para operaciones, liquidaciones, reportes y auditoria. Con estas escrituras no queda el usuario real que agrego/modifico/quitaro datos, se pierde trazabilidad historica de propietarios y un rechazo del servicio puede terminar como error tecnico. Esto incumple el estandar del proyecto: auditoria automatica de quien/cuando y evitar `DELETE` funcional salvo tablas detalle puramente transitorias y justificadas.

Solucion esperada: modelar las escrituras con entidades/JPA o un mecanismo comun que preserve `usuario_creacion/usuario_modificacion` reales. Para propietarios, agregar baja logica o historico equivalente antes de permitir "quitar"; reactivar si se agrega de nuevo sin duplicar. `ActivoBean.quitarPropietario()` debe capturar `NegocioException` y mostrar mensaje controlado. Para atributos, evitar borrar/actualizar via SQL nativo con `sistema`; si se decide borrar valores vacios, justificar que es valor transitorio y mantener auditoria real en altas/cambios.

Evidencia:

- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/ActivoService.java:161`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/ActivoService.java:164`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/ActivoService.java:169`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/ActivoService.java:244`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/ActivoService.java:252`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/ActivoBean.java:147`
- `Desarrollo/sginmo-web/src/main/resources/db/migration/V1__esquema_inicial.sql:343`

### Obs 213 - La jerarquia recursiva permite ciclos indirectos

Problema: `ActivoService.guardar()` solo valida que un activo no sea su propio contenedor directo (`padre == id`). No valida que el nuevo padre no sea descendiente del mismo activo. Por ejemplo, si B es hijo de A, la edicion de A puede asignar `padre=B` y crear el ciclo A -> B -> A.

Impacto: la tabla `activo` reemplaza entidades inmobiliarias/propiedades y sera base para propiedades, lotes, operaciones y reportes. Un ciclo rompe navegacion jerarquica, consultas recursivas, generacion masiva de lotes y cualquier reporte por contenedor. La UI de autocomplete excluye solo el propio activo, no sus descendientes.

Solucion esperada: antes de guardar, validar en servicio que `padre` no sea el propio activo ni ningun descendiente. La validacion debe estar en backend, no solo en autocomplete. Mensaje esperado: "El contenedor no puede ser el propio activo ni uno de sus descendientes" o equivalente.

Evidencia:

- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/ActivoService.java:59`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/ActivoService.java:127`
- `Desarrollo/sginmo-web/src/main/webapp/activos.xhtml:122`

## Pruebas

- Revision estatica de entidad, servicio, bean, converter, XHTML y migraciones.
- No se ejecuto build de cierre porque el REQ queda bloqueado por inspeccion funcional de persistencia/auditoria.
