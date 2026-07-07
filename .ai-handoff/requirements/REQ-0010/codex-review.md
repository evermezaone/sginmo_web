# Codex Review - REQ-0010

Fecha: 2026-07-07
Auditor: codex
Resultado: APROBADO

## Ronda 2

Se reaudito la correccion de la Obs 209 sobre modo solo lectura real en el ABM de usuarios.

Verificado correcto:

- `UsuarioBean.editar()` establece `soloLectura = !sesion.puede("usuarios", "EDITAR")` para usuarios existentes.
- `UsuarioBean.nuevo()` deja `soloLectura = false` y mantiene la alta protegida por permiso `CREAR`.
- `UsuarioBean.guardar()` retorna sin escribir si esta en modo solo lectura o si falta `CREAR`/`EDITAR`.
- `usuarios.xhtml` deshabilita campos de datos, password, grupos y permisos cuando `usuarioBean.soloLectura` es verdadero.
- `usuarios.xhtml` oculta `Guardar`, agregar/quitar grupos y agregar/quitar permisos con `rendered="#{!usuarioBean.soloLectura}"`.
- `desbloquear`, `eliminarPermiso` y `quitarGrupo` capturan `NegocioException` y muestran mensajes controlados.
- `UsuarioService` mantiene enforcement backend para `CREAR`, `EDITAR`, `INACTIVAR`, `REACTIVAR` y operaciones de seguridad.

Archivos revisados:

- `Desarrollo/onesystem-security/src/main/java/py/com/one/security/web/UsuarioBean.java`
- `Desarrollo/onesystem-security/src/main/resources/META-INF/resources/usuarios.xhtml`
- `Desarrollo/onesystem-security/src/main/java/py/com/one/security/servicio/UsuarioService.java`
- `.ai-handoff/requirements/REQ-0010/preaudit-checklist.md`

## Observaciones

Sin hallazgos bloqueantes pendientes.

Obs 209 queda corregida: el modo consulta ya no expone controles de escritura en el dialogo y las acciones sensibles mantienen bloqueo de servicio con manejo controlado en el bean.

## Pruebas

- Revision estatica de service, bean y XHTML.
- Build multi-modulo ejecutado desde `Desarrollo`:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-23'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& ..\herramientas\apache-maven-3.9.9\bin\mvn.cmd -q clean package
```

Resultado: EXIT 0.
