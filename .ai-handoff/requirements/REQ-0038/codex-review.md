# Codex Review - REQ-0038

Estado: REQUIERE_CAMBIOS
Fecha: 2026-07-09T19:44:00-03:00
Auditor: codex

## Observaciones bloqueantes

### Obs 257 - Permisos e integrantes de seguridad se modifican por id sin validar tenant

REQ-0038 exige que un ADMINISTRADOR no vea ni edite usuarios/grupos de otra empresa, incluidas asignaciones de grupo y permisos. Las grillas principales filtran por tenant, pero varias operaciones por id no reciben `actorTenant` ni validan pertenencia:

- `Desarrollo/onesystem-security/src/main/java/py/com/one/security/servicio/UsuarioService.java:204-210`: `quitarDeGrupo(usuarioGrupoId)` elimina cualquier `UsuarioGrupo` por id.
- `Desarrollo/onesystem-security/src/main/java/py/com/one/security/servicio/UsuarioService.java:285-291`: `eliminarPermiso(permisoId)` elimina cualquier `PermisoUsuario` por id.
- `Desarrollo/onesystem-security/src/main/java/py/com/one/security/servicio/GrupoService.java:151-169`: `agregarPermiso(grupoId, ...)` agrega permisos a cualquier grupo por id, incluyendo plantillas `tenant = -1` o grupos de otro tenant.
- `Desarrollo/onesystem-security/src/main/java/py/com/one/security/servicio/GrupoService.java:172-178`: `eliminarPermiso(permisoId)` elimina cualquier permiso de grupo por id.
- `Desarrollo/onesystem-security/src/main/java/py/com/one/security/servicio/GrupoService.java:143-147` y `:182-187`: `listarPermisos(grupoId)` / `listarIntegrantes(grupoId)` leen detalle por id sin validar que el grupo sea visible/editable.

Impacto: como `usuario`, `grupo`, `usuario_grupo` y permisos estan excluidos de RLS, esta es la unica barrera. Un request manipulado con ids conocidos puede quitar integrantes o permisos de usuarios/grupos de otra empresa, o modificar permisos de una plantilla global `-1` aunque el REQ dice que son solo lectura para ADMINISTRADOR.

Solucion esperada: todas esas operaciones deben recibir `actorTenant` y validar pertenencia antes de leer/escribir. Para ADMINISTRADOR: usuario objetivo debe ser de su tenant, grupo debe ser propio para editar permisos, plantillas `-1` solo asignables/no editables, y permisos/integrantes deben resolverse hasta su usuario/grupo dueño antes de eliminar. SUPERADMIN mantiene acceso global.

### Obs 258 - EmpresaService.guardar permite crear empresas fuera del alta atomica de SUPERADMIN

REQ-0038 define que el alta de empresa como unidad es una operacion de SUPERADMIN: crea persona juridica + rol EMPRESA + datos comerciales + sucursal por defecto + usuario ADMINISTRADOR inicial en una sola transaccion. Sin embargo el service conserva una ruta de creacion parcial:

- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/EmpresaService.java:108-153`: `guardar(PersonaJuridica, PersonaEmpresa)` permite crear una empresa y su rol EMPRESA sin exigir `tenant.esSuperadmin()` y sin crear sucursal por defecto ni usuario administrador inicial.
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/EmpresaBean.java:104-110`: si `isAltaUnidad()` es falso, el bean llama a `empresaService.guardar(...)`; para un usuario no SUPERADMIN con permiso `empresas/CREAR`, esto abre la ruta parcial.

Impacto: se puede crear una empresa incompleta, sin admin inicial y sin sucursal por defecto, rompiendo el criterio de alta atomica y dejando datos multiempresa a medio provisionar. Aunque hoy la UI pueda ocultar permisos, la regla critica debe estar en backend.

Solucion esperada: bloquear en `EmpresaService.guardar()` la creacion de empresas nuevas para no SUPERADMIN, o convertir toda creacion nueva a `altaEmpresa(...)`. Dejar `guardar(...)` solo para edicion de empresas existentes, con guardas de permiso/rol claras.

## Verificacion realizada

- Leidos `req.md`, `claude-implementation.md`, `preaudit-checklist.md` y `test-plan.md`.
- Inspeccionados `EmpresaService`, `EmpresaBean`, `TenantContext`, `SuperadminBean`, `ContextoEmpresa`, `UsuarioService`, `GrupoService`, `UsuarioBean`, `GrupoBean` y referencias XHTML de usuarios/grupos.
- Comparado contra criterios F6: alta atomica por SUPERADMIN, aislamiento app-layer para seguridad fuera de RLS, plantillas `-1` solo lectura para ADMINISTRADOR.

No se ejecuta build como criterio de aprobacion porque el REQ queda rechazado por brechas backend de aislamiento y alta atomica.

---

## Reauditoria - 2026-07-09T19:46:51-03:00

Estado: REQUIERE_CAMBIOS

### Obs 257 - Parcialmente cerrada; queda Obs 259

Verificado:

- `UsuarioService.listarGruposDe`, `listarPermisos`, `quitarDeGrupo` y `eliminarPermiso` ahora reciben `actorTenant` y validan el usuario propietario.
- `GrupoService.listarPermisos`, `agregarPermiso`, `eliminarPermiso` y `listarIntegrantes` ahora reciben `actorTenant`.
- `GrupoService.agregarPermiso`/`eliminarPermiso` usan `exigirGrupoEditable`, por lo que un ADMINISTRADOR ya no edita grupos ajenos ni plantillas `-1`.
- `UsuarioBean` y `GrupoBean` pasan `sesion.tenantActual()` en las llamadas corregidas.

Pero `GrupoService.listarIntegrantes(grupoId, actorTenant)` solo valida que el grupo sea visible. Para una plantilla global `tenant = -1`, visible para cualquier ADMINISTRADOR, devuelve todos los `UsuarioGrupo` de ese grupo sin filtrar el usuario por tenant:

- `Desarrollo/onesystem-security/src/main/java/py/com/one/security/servicio/GrupoService.java:204-209`
- `Desarrollo/onesystem-security/src/main/java/py/com/one/security/web/GrupoBean.java:99-102`
- `Desarrollo/onesystem-security/src/main/resources/META-INF/resources/grupos.xhtml:143-171`

Impacto: al abrir una plantilla global asignable, un ADMINISTRADOR puede ver integrantes de otros tenants (aunque sea como ids cuando `usuariosPorId` no contiene el usuario). `usuario_grupo` no tiene tenant propio y seguridad esta fuera de RLS, asi que el service debe filtrar por el tenant del usuario asociado.

Solucion esperada: para ADMINISTRADOR, `listarIntegrantes` debe devolver solo integrantes cuyo `Usuario.tenant = actorTenant`. Para SUPERADMIN global puede devolver todos. Si se prefiere que las plantillas sean solo asignables desde Usuarios, la pestaña Integrantes de grupos `-1` debe ocultarse o quedar vacia para ADMINISTRADOR.

### Obs 258 - Cerrada

Verificado:

- `EmpresaService.guardar(...)` ahora rechaza `empresa.getId() == null`.
- La creacion parcial fue movida a `crearEmpresa(...)`, privado.
- `altaEmpresa(...)` sigue siendo la ruta publica de alta completa y exige `tenant.esSuperadmin()`.

### Obs 260 - El selector "operar como" no acota los ABM de seguridad

REQ-0038 exige que cuando el SUPERADMIN opera como una empresa, TODO el sistema quede acotado a ese tenant. Sin embargo los ABM de `onesystem-security` no usan `TenantContext.actual()`: usan `SesionUsuario.tenantActual()`, que devuelve siempre el tenant real del usuario logueado.

- `Desarrollo/onesystem-security/src/main/java/py/com/one/security/web/SesionUsuario.java:60-63`: `tenantActual()` retorna `usuario.getTenant()`; no conoce el override de soporte.
- `Desarrollo/onesystem-security/src/main/java/py/com/one/security/web/UsuarioBean.java:67-78`, `:117-118`, `:137-138`, `:163-165`, `:174-175`, `:191-202`: usuarios usa `sesion.tenantActual()` para filtrar y escribir.
- `Desarrollo/onesystem-security/src/main/java/py/com/one/security/web/GrupoBean.java:66-72`, `:101-102`, `:113`, `:129`, `:142-143`, `:151-152`, `:159-169`: grupos usa `sesion.tenantActual()` para filtrar y escribir.

Impacto: un SUPERADMIN real tiene `usuario.tenant = -1`; aunque elija "operar como" una empresa en `TenantContext`, los ABM de Usuarios/Grupos siguen recibiendo `actorTenant = -1`, ven todo y editan como SUPERADMIN global. Eso contradice el criterio de soporte: el contexto entero debe acotarse mientras dura el override.

Solucion esperada: conectar los ABM de seguridad al tenant efectivo. Puede ser inyectando un proveedor/bridge de tenant efectivo desde la aplicacion anfitriona, o usando `TenantContext.actual()` directamente en SGInmo para pasar `actorTenant`. El tenant real `-1` solo debe usarse para autorizar que el usuario puede cambiar el override; las operaciones comunes deben recibir el tenant efectivo.

No se ejecuta build como criterio de aprobacion porque el REQ sigue rechazado por brechas de aislamiento en seguridad fuera de RLS.

---

## Reauditoria - 2026-07-09T19:59:51-03:00

Estado: APROBADO_POR_CODEX

### Observaciones cerradas

- Obs 257: cerrada. Las operaciones por id de usuarios/grupos/permisos reciben `actorTenant` y validan usuario o grupo propietario antes de leer/escribir.
- Obs 258: cerrada. `EmpresaService.guardar(...)` rechaza altas nuevas parciales; el alta completa queda en `altaEmpresa(...)`.
- Obs 259: cerrada. `GrupoService.listarIntegrantes(grupoId, actorTenant)` filtra por `Usuario.tenant = actorTenant` cuando el actor no es SUPERADMIN, incluso para plantillas `tenant = -1`.
- Obs 260: cerrada. El override de soporte vive en `SesionUsuario`; `TenantContext.actual()` delega en `sesion.tenantActual()` y los ABM de seguridad usan ese mismo tenant efectivo.

### Verificacion realizada

- Inspeccionados `SesionUsuario`, `TenantContext`, `UsuarioBean`, `GrupoBean`, `UsuarioService`, `GrupoService`, `EmpresaService` y el flujo de selector SUPERADMIN.
- Confirmado que un ADMINISTRADOR ve plantillas `-1` pero no puede editar sus permisos ni ver integrantes de otros tenants.
- Confirmado que un SUPERADMIN con override deja de operar como global para los ABM de seguridad, porque `sesion.tenantActual()` retorna el tenant impersonado.
- Build: `mvn -q -pl sginmo-web -am clean package` en `Desarrollo` -> EXIT 0.
