# Codex Review - REQ-0004

Fecha: 2026-07-06
Auditor: codex
Resultado: REQUIERE_CAMBIOS

## Hallazgos

### 1. Build multi-modulo roto

Se ejecuto desde `Desarrollo/`:

```powershell
..\herramientas\apache-maven-3.9.9\bin\mvn.cmd -q clean package
```

Resultado: falla de compilacion en `onesystem-security`, con errores en:

- `src/main/java/py/com/one/security/dominio/Grupo.java`
- `PermisoGrupo.java`
- `PermisoUsuario.java`
- `PreferenciaUsuario.java`
- `Usuario.java`
- `UsuarioGrupo.java`

El error apunta a expresiones `instanceof Tipo otro` en `equals`. Mientras esto falle, no hay WAR/JAR verificable y no se puede aprobar seguridad.

### 2. Permisos aplicados en beans, no en servicios transaccionales

La revision encontro controles `sesion.puede(...)` en beans JSF (`ArticuloBean`, `UsuarioBean`, `GrupoBean`, etc.), pero los servicios con escrituras (`ArticuloService`, `UsuarioService`, `GrupoService` y servicios de catalogo) no validan permisos ni usan `@RolesAllowed`/interceptor equivalente.

Esto replica el riesgo explicitamente prohibido por `CODEX.md`: autorizacion solo en UI. La UI puede ocultar botones, pero la autorizacion debe proteger tambien la capa que transacciona.

## Verificacion adicional

- Leidos `req.md`, `claude-implementation.md`, `test-plan.md` y `preaudit-checklist.md`.
- Revisados `SeguridadService`, `FiltroAutenticacion`, `LoginBean`, `SesionUsuario`, `UsuarioService`, `GrupoService`, `ArticuloBean`, `ArticuloService`.
- El login/bloqueo usa bcrypt y `dontRollbackOn`, pero no se audita funcionalmente hasta que el build compile.

## Decision

REQ-0004 debe volver a Claude con cambios.
