# REQ-0064 - Plan De Implementacion

**Estado:** APROBADO_PARA_DESARROLLO
**Fecha:** 2026-07-12

## Estrategia

Reforzar la seguridad EXISTENTE (bcrypt + intentos/bloqueo ya configurables) con: params visibles,
auditoria de login (login_evento), anti-reuse (password_historial) y desbloqueo admin. Cambios aditivos
y fail-safe en SeguridadService para no romper el login.

## Archivos A Modificar

| Archivo | Cambio |
|---|---|
| V45__politicas_seguridad.sql | params + login_evento + password_historial + pantalla |
| onesystem-security SeguridadService.java | validarNueva config + log login + anti-reuse |
| servicio/SeguridadPoliticaService.java | NUEVO — politicas/bloqueados/desbloquear/auditoria |
| web/SeguridadBean.java + webapp/seguridad.xhtml | NUEVOS |
| WEB-INF/plantilla.xhtml + smoke | menu + cobertura |

## Pruebas Previstas

- [ ] Build OK
- [ ] V45 rollback + deploy + smoke (login OK)
- [ ] login_evento se pobla; anti-reuse; desbloqueo con permiso

## Riesgos

- Toca autenticacion: mitigado (log fail-safe; smoke verifica login).

## Cambios De Datos

V45 params LOGIN_* + login_evento + password_historial + pantalla.
