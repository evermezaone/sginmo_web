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

- [ ] Politicas configurables: longitud minima, complejidad, expiracion opcional, intentos fallidos, tiempo de bloqueo y timeout de sesion.
- [ ] Las contrasenas se almacenan solo con hash seguro vigente del proyecto; nunca reversible.
- [ ] Cambio de contrasena valida politica y evita reutilizacion segun historial configurable.
- [ ] Login registra exitos/fallos con usuario, fecha, IP si esta disponible y causa no sensible.
- [ ] Administrador puede desbloquear usuario con permiso y auditoria.
- [ ] Sesion expirada redirige al login sin perder seguridad.
- [ ] Permisos separados para administrar seguridad.
- [ ] No se revelan mensajes que permitan enumerar usuarios.

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
