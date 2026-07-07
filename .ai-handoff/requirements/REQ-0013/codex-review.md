# Codex Review - REQ-0013

Fecha: 2026-07-07
Auditor: codex
Resultado: APROBADO

## Ronda 2

Se reauditaron las correcciones de Obs 212 y Obs 213 sobre activos inmobiliarios.

Verificado correcto:

- `ActivoPropietario` ahora tiene `estado` (`ACTIVO`/`INACTIVO`) y migracion `V22__activo_propietario_estado.sql` idempotente.
- `ActivoService.propietariosDe()` y `propietariosConId()` filtran solo propietarios activos.
- `ActivoService.agregarPropietario()` usa JPA, no duplica, y reactiva un propietario inactivo existente.
- `ActivoService.quitarPropietario()` ya no hace `DELETE`; aplica baja logica con `estado = INACTIVO`.
- `ActivoBean.quitarPropietario()` captura `NegocioException` y muestra mensaje controlado.
- `ActivoAtributo` se modelo como entidad auditable; altas/cambios de valores de atributo pasan por JPA.
- `ActivoService.buscarContenedor()` excluye el activo actual y sus descendientes.
- `ActivoService.guardar()` valida en backend que el padre no sea el propio activo ni un descendiente, evitando ciclos indirectos.

Archivos revisados:

- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/dominio/activo/ActivoPropietario.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/dominio/activo/ActivoAtributo.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/ActivoService.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/ActivoBean.java`
- `Desarrollo/sginmo-web/src/main/resources/db/migration/V22__activo_propietario_estado.sql`

## Observaciones

Sin hallazgos bloqueantes pendientes.

Obs 212 queda corregida: propietarios preservan trazabilidad por baja logica y las escrituras relevantes usan JPA/auditoria real.

Obs 213 queda corregida: la jerarquia recursiva valida descendientes en backend y en el autocomplete.

## Pruebas

- Revision estatica de entidades, servicio, bean y migracion.
- Build multi-modulo ejecutado desde `Desarrollo`:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-23'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& ..\herramientas\apache-maven-3.9.9\bin\mvn.cmd -q clean package
```

Resultado: EXIT 0.
