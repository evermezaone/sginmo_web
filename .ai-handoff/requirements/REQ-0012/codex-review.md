# Codex Review - REQ-0012

Fecha: 2026-07-07
Auditor: codex
Resultado: REQUIERE_CAMBIOS

## Verificacion

- Leidos `req.md`, `claude-implementation.md`, `test-plan.md` y `preaudit-checklist.md`.
- Revisados:
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/dominio/persona/Persona.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/dominio/persona/PersonaFisica.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/dominio/persona/PersonaJuridica.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/dominio/persona/PersonaRol.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/PersonaService.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/PersonaBean.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/PersonaConverter.java`
  - `Desarrollo/sginmo-web/src/main/webapp/personas.xhtml`
  - `Desarrollo/sginmo-web/src/main/resources/db/migration/V1__esquema_inicial.sql`
  - `Desarrollo/sginmo-web/src/main/resources/db/migration/V14__pantalla_personas.sql`

Confirmado correcto:

- El ABM usa `LazyDataModel`, filtro global y whitelist de orden en `PersonaService`.
- `PersonaFisica` y `PersonaJuridica` usan `@MapsId` con cascade `PERSIST`/`MERGE` hacia `Persona`.
- `PersonaService.guardarFisica()` y `guardarJuridica()` son transaccionales y guardan persona base + subtipo.
- `numero_documento` es unico global y el servicio valida duplicados con mensaje de negocio.
- `PersonaService.porRol()` devuelve personas activas con rol activo para combos de otros modulos.
- La pantalla aplica modo solo lectura a campos y oculta Guardar/agregar/quitar roles.
- Hay converter administrado por id para combos de persona.
- El build multi-modulo ejecutado durante este ciclo termino EXIT 0.

## Hallazgos Bloqueantes

### Obs 211 - Quitar rol de persona hace DELETE fisico y pierde trazabilidad

Problema: `PersonaService.quitarRol()` ejecuta `em.remove(r)` sobre `persona_rol`, aunque la tabla tiene columna `estado` y el estandar del proyecto exige baja logica, no DELETE, para conservar historial. La UI expone esta accion con el boton de papelera en la pestaña Roles. Ademas `PersonaBean.quitarRol()` no captura `NegocioException`, a diferencia de `agregarRol()`, por lo que un rechazo del servicio puede terminar como error tecnico.

Impacto: una persona puede ser cliente/proveedor/propietario/inquilino en distintos momentos. Borrar fisicamente el rol elimina evidencia historica de esa relacion y puede romper auditoria o explicacion de datos ya usados por operaciones, cobros, activos o reportes. Tambien deja inconsistente el modelo: `persona_rol.estado` existe pero no se usa para inactivar/reactivar roles.

Solucion esperada: cambiar `quitarRol()` para baja logica (`estado='INACTIVO'`) y ajustar consultas:

- `rolesDe()` debe mostrar claramente solo roles activos o separar activos/inactivos segun UI decidida;
- `agregarRol()` debe reactivar un rol inactivo existente en vez de duplicarlo o bloquearlo sin salida;
- `PersonaBean.quitarRol()` debe capturar `NegocioException` y mostrar mensaje controlado;
- mantener `porRol()` filtrando `PersonaRol.estado='ACTIVO'`.

Evidencia:

- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/PersonaService.java:175`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/PersonaService.java:178`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/PersonaBean.java:174`
- `Desarrollo/sginmo-web/src/main/webapp/personas.xhtml:255`
- `Desarrollo/sginmo-web/src/main/resources/db/migration/V1__esquema_inicial.sql:245`

## Pruebas

- Revision estatica de entidades, servicio, bean, converter, XHTML y migraciones.
- Build multi-modulo ejecutado desde `Desarrollo`:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-23'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& ..\herramientas\apache-maven-3.9.9\bin\mvn.cmd -q clean package
```

Resultado: EXIT 0.

No se aprueba por el DELETE fisico de roles de persona.
