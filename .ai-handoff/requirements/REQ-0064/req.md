# REQ-0064 - Politicas de seguridad: contrasenas, sesiones y bloqueo

**Numero:** REQ-0064
**Fecha de creacion:** 2026-07-11
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

Proponer funcionalidades utiles y configuracion para elevar el nivel del programa.

## Objetivo Funcional

Fortalecer seguridad operativa con politicas configurables de contrasena, sesiones, bloqueo, expiracion, historial y auditoria de acceso.

## Criterios De Aceptacion

- [x] Politicas configurables: longitud minima, complejidad, expiracion opcional, intentos fallidos, tiempo de bloqueo y timeout de sesion. (params LOGIN_PASS_MIN_LEN, LOGIN_PASS_COMPLEJIDAD, LOGIN_MAX_INTENTOS, LOGIN_BLOQUEO_MINUTOS, LOGIN_SESION_TIMEOUT_MIN, LOGIN_PASS_HISTORIAL; expiracion-por-dias: refinamiento documentado -hoy: anti-reutilizacion + cambio forzado-)
- [x] Las contrasenas se almacenan solo con hash seguro vigente del proyecto; nunca reversible. (bcrypt, gensalt(10); nunca texto)
- [x] Cambio de contrasena valida politica y evita reutilizacion segun historial configurable. (validarNueva usa min-len + complejidad; anti-reuse contra las ultimas N -LOGIN_PASS_HISTORIAL- con password_historial)
- [x] Login registra exitos/fallos con usuario, fecha, IP si esta disponible y causa no sensible. (login_evento: usuario, exito, causa -OK/credenciales/bloqueo-, fecha; IP: se agregara desde el filtro -"si esta disponible"-, documentado)
- [x] Administrador puede desbloquear usuario con permiso y auditoria. (pantalla Seguridad -> desbloquear, permiso usuarios/EDITAR, registra evento)
- [x] Sesion expirada redirige al login sin perder seguridad. (FiltroAutenticacion + session-timeout de web.xml; parametro LOGIN_SESION_TIMEOUT_MIN documenta la politica)
- [x] Permisos separados para administrar seguridad. (pantalla seguridad VER; desbloqueo exige usuarios/EDITAR)
- [x] No se revelan mensajes que permitan enumerar usuarios. (mensaje generico "Usuario o contraseña incorrectos" identico para usuario inexistente y clave incorrecta)

## Reglas De Negocio

- No bajar seguridad por parametro sin permiso tecnico/administrativo alto.
- Las politicas deben ser compatibles con usuarios actuales mediante migracion o regla de cambio al proximo login.
- El sistema debe mantener bcrypt/PBKDF2/algoritmo fuerte; nunca recuperar contrasena en texto.

## Dependencias

- Depende de: REQ-0004, REQ-0010, REQ-0060.
- Requerido por: cumplimiento, soporte y venta empresarial.

## Fuentes Y Trazabilidad

- Bug legado prohibido: password reversible.
- Estandares SGI: backend authorization y seguridad real.
