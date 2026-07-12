# REQ-0064 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` (con onesystem-security) | Build OK | OK |
| T02 | V45 en `BEGIN...ROLLBACK` | params + 2 tablas + pantalla | OK |
| T03 | Deploy + Flyway V45 | success=t | OK |
| T04 | `python tools/smoke-test-vps.py` (1er intento) | render OK | seguridad dio 500 (timestamptz de query nativa != LocalDateTime) |
| T05 | Fix (formatear timestamps en el servicio) + smoke | 30/30 render OK | OK (TODAS OK) |
| T06 | Login del smoke registra evento | login_evento poblado; login OK | OK (eventos=1, exito=1) |

## Pruebas Manuales

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | Fallar login N veces | usuario bloqueado; visible en Seguridad | pendiente |
| M02 | Desbloquear (admin) | usuario desbloqueado + evento auditado | pendiente |
| M03 | Cambiar clave por una reciente | rechazo anti-reuse | pendiente |
| M04 | Cambiar clave corta / sin complejidad (si activada) | rechazo por politica | pendiente |

## Revision Transversal (SEGURIDAD)

- bcrypt vigente (gensalt 10); nunca reversible; no se loguea la contrasena.
- Log de login fail-safe (try/catch): nunca bloquea el acceso; dontRollbackOn conserva el fallo.
- Anti-reuse: bcrypt.checkpw contra las ultimas N; fail-open si el historial no esta disponible (documentado).
- No enumeracion: mensaje generico identico (preexistente).
- Desbloqueo: exige usuarios/EDITAR + audita en login_evento.
- Se toco autenticar/cambiarPassword (criticos): smoke confirma que el login sigue OK; el cambio de clave no se
  ejercita en el smoke (accion aparte), revisado por codigo.

## Datos De Prueba

Un usuario de prueba para bloquear/desbloquear y cambiar contrasena.
