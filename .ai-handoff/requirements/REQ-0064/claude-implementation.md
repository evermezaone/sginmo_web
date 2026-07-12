# REQ-0064 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0064
- Tipo de cambio: BD (params + 2 tablas + pantalla) + backend (SEGURIDAD, onesystem-security) + UI
- Riesgo: ALTO (toca autenticacion y cambio de contrasena)
- Archivos clave:
  - `V45__politicas_seguridad.sql`: params de seguridad (LOGIN_*) + `login_evento` (auditoria) + `password_historial` (anti-reuse) + pantalla `seguridad`.
  - `onesystem-security/.../SeguridadService.java`: validarNueva ahora usa LOGIN_PASS_MIN_LEN + LOGIN_PASS_COMPLEJIDAD; autenticar registra login_evento (fail-safe); cambiarPassword valida anti-reuse contra las ultimas N (LOGIN_PASS_HISTORIAL) y registra el hash.
  - `servicio/SeguridadPoliticaService.java` (sginmo-web): politicas, usuariosBloqueados, desbloquear (permiso usuarios/EDITAR + audit), eventosRecientes.
  - `web/SeguridadBean.java` + `webapp/seguridad.xhtml`: politicas + desbloqueo + auditoria.
  - `WEB-INF/plantilla.xhtml`, `tools/smoke-test-vps.py`: menu + cobertura.
- Comandos probados:
  - `mvn -q clean package` (con onesystem-security): BUILD OK.
  - V45 en `BEGIN...ROLLBACK`: params + 2 tablas + pantalla.
  - Deploy + Flyway V45 `success=t`; `python tools/smoke-test-vps.py`: 30/30 RENDER OK incl. `seguridad`.
  - login_evento se poblo con el login del smoke (exito=1) -> el login SIGUE funcionando tras tocar autenticar.
- Cambios de datos: si, V45 (params + tablas + pantalla).
- Cambios de entorno: no.
- Impacto LLM/tokens: no.
- Decision esperada: revisar SEGURIDAD (autenticacion/cambio de clave) con cuidado.
- Notas para auditor (SEGURIDAD):
  - bcrypt vigente (gensalt 10); nunca reversible; nunca se loguea la contrasena.
  - Log de login fail-safe (try/catch): una falla de auditoria NUNCA bloquea el acceso. dontRollbackOn conserva el registro del intento fallido.
  - Anti-reuse: compara contra las ultimas N (bcrypt.checkpw); si el historial no esta disponible, no bloquea el cambio legitimo (fail-open documentado).
  - No enumeracion: mensaje generico identico para usuario inexistente y clave incorrecta (preexistente).
  - Desbloqueo: exige permiso usuarios/EDITAR y audita.

## Resumen Funcional

Nueva pantalla "Seguridad": muestra las politicas configurables (parametros LOGIN_*), los usuarios
bloqueados (con desbloqueo) y la auditoria de accesos (exitos/fallos). El login registra cada intento;
el cambio de contrasena valida longitud/complejidad y evita reutilizar las ultimas N.

## Resumen Tecnico

Politicas via ParametroConfig/SPI (ya usado por autenticar). login_evento + password_historial nuevas
(sin RLS, admin-only). SeguridadService enriquecido (validarNueva, registrarLogin, anti-reuse).

## Limitaciones Conocidas (transparencia)

- Expiracion de contrasena por dias: DIFERIDA; hoy: anti-reutilizacion + cambio forzado (debe_cambiar_password) + complejidad.
- IP en login_evento: se capturara desde un filtro (hoy null; el criterio dice "si esta disponible").
- Timeout de sesion efectivo: web.xml (30 min); el parametro LOGIN_SESION_TIMEOUT_MIN documenta la politica.

## Archivos Modificados

Ver Manifiesto. V45 nueva; SeguridadService (onesystem-security) enriquecido.

## Cambios De Datos

V45: params LOGIN_* + tablas login_evento y password_historial + pantalla seguridad.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; V45 rollback OK; deploy + Flyway success; smoke 30/30; login_evento poblado (login OK). Ver test-plan.

## Pruebas Manuales Sugeridas

1. Fallar el login N veces -> usuario bloqueado; ver en Seguridad -> desbloquear. Ver auditoria de accesos.
2. Cambiar contrasena por una reciente -> rechazo por anti-reuse.

## Riesgos Conocidos

- Toca autenticacion/cambio de clave: mitigado (fail-safe log, smoke confirma login OK). Ver "Limitaciones".
