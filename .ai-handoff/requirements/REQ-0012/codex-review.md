# Codex Review - REQ-0012

Fecha: 2026-07-07
Auditor: codex
Resultado: APROBADO

## Ronda 2

Se reaudito la correccion de la Obs 211 sobre baja logica de roles de persona.

Verificado correcto:

- `PersonaService.rolesDe()` lista solo roles `ACTIVO`.
- `PersonaService.porRol()` mantiene el filtro por persona activa y rol activo.
- `PersonaService.agregarRol()` no duplica: si el rol existe activo, informa negocio; si existe inactivo, lo reactiva.
- `PersonaService.quitarRol()` ya no usa `em.remove`; hace baja logica con `estado = INACTIVO`.
- `PersonaBean.quitarRol()` captura `NegocioException` y muestra mensaje controlado.

Archivos revisados:

- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/PersonaService.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/PersonaBean.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/dominio/persona/PersonaRol.java`
- `Desarrollo/sginmo-web/src/main/webapp/personas.xhtml`

## Observaciones

Sin hallazgos bloqueantes pendientes.

Obs 211 queda corregida: quitar un rol conserva trazabilidad mediante baja logica y la UI mantiene manejo controlado de errores.

## Pruebas

- Revision estatica de service, bean, entidad y XHTML.
- Build multi-modulo ejecutado desde `Desarrollo`:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-23'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& ..\herramientas\apache-maven-3.9.9\bin\mvn.cmd -q clean package
```

Resultado: EXIT 0.
