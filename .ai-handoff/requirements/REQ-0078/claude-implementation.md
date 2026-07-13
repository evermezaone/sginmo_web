# REQ-0078 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-13
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0078
- Tipo de cambio: BD (V52: credenciales + OTP + auditoria) + backend (PortalAuthService, PortalSesion) + UI publica (portal/login|otp|clave) + retiro del acoplamiento a `usuario.perfil='PORTAL'`.
- Riesgo: alto (autenticacion externa, RLS, aislamiento por persona/tenant).
- Archivos clave:
  - `V52__portal_externo_credenciales.sql`: `persona_portal_credencial` (bcrypt, intentos, bloqueo) y `portal_otp` (hash bcrypt, uso unico, expiracion, intentos) con RLS por tenant; amplia `portal_acceso` (nuevas acciones de seguridad + `user_agent`); parametros PORTAL_OTP_EXPIRA_MIN / PORTAL_OTP_LARGO / PORTAL_OTP_MAX_INTENTOS / PORTAL_LOGIN_MAX_INTENTOS / PORTAL_BLOQUEO_MIN (defaults -1, override por empresa).
  - `servicio/PortalAuthService.java` (NO @AislarTenant; fija app.tenant explicitamente por empresa elegida): empresas(), solicitarOtp(), validarOtp(), definirPassword(), loginPassword(), auditarLogout(). Identidad por (tenant, persona) con rol CLIENTE/PROPIETARIO ACTIVO; mensajes genericos (no revela existencia); OTP y password en bcrypt; limite de intentos + bloqueo temporal; auditoria por evento.
  - `web/PortalSesion.java` (@SessionScoped): sesion de socio (tenant/persona/roles), independiente de `SesionUsuario`; estado "pendiente" post-OTP para definir clave.
  - `web/PortalLoginBean.java` (@ViewScoped): flujo login por clave y primer-ingreso/recuperacion por OTP + definicion de clave.
  - `web/TenantContext.java`: cuando NO hay login administrativo pero SI socio autenticado, el tenant efectivo es el de su empresa -> la RLS (via interceptor @AislarTenant) acota PortalService a esa empresa.
  - `web/PortalBean.java`: la cuenta del socio ahora toma la identidad de PortalSesion (no de usuario.perfil='PORTAL'); logout audita e invalida sesion.
  - `onesystem-security/.../FiltroAutenticacion.java`: rutas `/portal/**` publicas respecto del login administrativo (su propio viewAction controla el acceso del socio).
  - `web/InicioBean.java`: `guardPortal()` retirado (el portal ya no usa el login de empleados).
  - `webapp/portal/login.xhtml|otp.xhtml|clave.xhtml` + `WEB-INF/portal-acceso.xhtml`: pantallas publicas responsive.
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - Deploy + Flyway V52 (schema now v52, "Successfully applied 1 migration"); `python tools/smoke-test-vps.py`: 36/36.
  - Prueba funcional en VPS (navegador): login publico 200; guardas de otp/clave/inicio redirigen a /portal/login (302) sin sesion; flujo empresa+documento -> OTP (solicitarOtp + buscarElegible SQL OK, mensaje generico sin revelar); validar codigo invalido -> mensaje generico (validarOtp SQL OK).
- Cambios de datos: si, V52 (2 tablas + RLS + ampliacion portal_acceso + 5 parametros). Cambios de entorno: no (OTP por email usa SMTP_* ya existentes; sin SMTP solo se audita).
- Decision esperada: aprobar; el mecanismo principal es CI/RUC + OTP + password de persona, NO `Perfil PORTAL + persona vinculada`.
- Notas para auditor:
  - Aislamiento: PortalAuthService fija app.tenant por la empresa elegida; PortalService (@AislarTenant) queda acotado por RLS al tenant del socio y ademas filtra por persona autenticada (nunca por id de request).
  - Anti-enumeracion: un unico mensaje generico para documento inexistente / rol no habilitado / password incorrecta; el envio de OTP es silencioso.
  - Fuerza bruta: intentos_fallidos + bloqueado_hasta en credencial (login) e intentos + invalidacion en OTP; parametros configurables por empresa.
  - Secretos: OTP y password bcrypt (jbcrypt, cost 10); nunca en texto plano; OTP de uso unico y expiracion.
  - Auditoria: SOLICITUD_OTP / VALIDACION_OTP / OTP_FALLIDO / LOGIN / LOGIN_FALLIDO / CAMBIO_PASSWORD / LOGOUT con persona, tenant, ip y user_agent.

## Resumen Funcional

Portal externo de socios separado del login de empleados: el cliente/propietario elige su empresa,
ingresa su CI/RUC y accede con contrasena propia; en el primer ingreso (o recuperacion) recibe un OTP,
lo valida y define su contrasena. Ya autenticado ve solo su cuenta (cuotas, pagos, documentos), aislada
por persona y empresa.

## Resumen Tecnico

Identidad de portal por (tenant, persona) con rol comercial, credencial y OTP hasheados (bcrypt) en
tablas propias con RLS; sesion de socio independiente de SesionUsuario; el tenant efectivo alimenta la
RLS de las consultas del portal. El perfil administrativo PORTAL deja de condicionar el acceso.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| V52__portal_externo_credenciales.sql | NUEVO - persona_portal_credencial + portal_otp + RLS + params + ampliacion portal_acceso |
| servicio/PortalAuthService.java | NUEVO |
| web/PortalSesion.java, web/PortalLoginBean.java | NUEVOS |
| web/PortalBean.java | identidad desde PortalSesion (no perfil PORTAL) |
| web/TenantContext.java | fallback de tenant a la sesion de portal |
| web/InicioBean.java | guardPortal() retirado |
| onesystem-security/.../FiltroAutenticacion.java | /portal/** publico |
| webapp/portal/login.xhtml, otp.xhtml, clave.xhtml, WEB-INF/portal-acceso.xhtml | NUEVAS pantallas publicas |

## Cambios De Datos

V52: `persona_portal_credencial` y `portal_otp` (RLS por tenant); `portal_acceso` ampliada (acciones + user_agent); 5 parametros PORTAL_*.

## Variables De Entorno

Sin cambios. El OTP por email reutiliza SMTP_* (si no hay SMTP, el OTP queda auditado sin enviarse; SMS sin gateway queda auditado).

## Pruebas Ejecutadas

Build OK; Flyway V52 (schema v52); smoke 36/36; prueba funcional en VPS de login publico, guardas de sesion y ramas de OTP no-encontrado/invalido (SQL de elegibilidad y OTP ejecutan sin error, con mensaje generico).

## Pruebas Manuales Sugeridas

1. Con un socio real (persona ACTIVA + rol CLIENTE/PROPIETARIO ACTIVO + email registrado y SMTP configurado): primer ingreso -> recibe OTP -> define clave -> entra a su cuenta.
2. Login posterior con CI/RUC + clave; luego con clave incorrecta repetida -> bloqueo temporal.
3. Recuperacion: "Primer ingreso o olvide mi contrasena" -> OTP -> nueva clave.
4. Aislamiento: un socio nunca ve datos de otro socio ni de otra empresa (RLS + filtro por persona).

## Limitaciones Conocidas

- Envio de OTP por SMS: modelo y canal preparados (columna canal), sin gateway integrado (queda auditado); email via SMTP_* existente.
- Vista de propietario: reutiliza las consultas del REQ-0055 (cuenta del socio); ampliar el detalle de activos/liquidaciones del propietario es incremental.
- La empresa se elige en el login (no hay subdominio por empresa); resuelve el tenant sin ambiguedad multi-tenant.

## Riesgos Conocidos

- Autenticacion externa: mitigado con bcrypt, OTP de uso unico/expiracion, limites de intentos + bloqueo, mensajes genericos, auditoria y RLS por tenant + filtro por persona.
