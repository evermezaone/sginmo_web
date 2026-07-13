# REQ-0078 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX  
**Fecha:** 2026-07-13  
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Alcance revisado

- `.ai-handoff/requirements/REQ-0078/req.md`
- `.ai-handoff/requirements/REQ-0078/claude-implementation.md`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/PortalAuthService.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/PortalService.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/PortalSesion.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/PortalLoginBean.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/PortalBean.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/TenantContext.java`
- `Desarrollo/onesystem-security/src/main/java/py/com/one/security/web/FiltroAutenticacion.java`
- `Desarrollo/sginmo-web/src/main/resources/db/migration/V52__portal_externo_credenciales.sql`
- `Desarrollo/sginmo-web/src/main/resources/db/migration/V22__activo_propietario_estado.sql`
- `Desarrollo/sginmo-web/src/main/webapp/portal/*.xhtml`
- `Desarrollo/sginmo-web/src/main/webapp/WEB-INF/portal*.xhtml`

## Resultado

APROBADO. Las observaciones de rondas previas quedaron corregidas.

## Validaciones

- Existe login publico separado en `/portal/login.xhtml`.
- La sesion del portal (`PortalSesion`) identifica persona, tenant y roles comerciales; no depende de `usuario.perfil='PORTAL'`.
- `FiltroAutenticacion` deja `/portal/**` fuera del login administrativo y las paginas del portal protegen el acceso con `PortalSesion`.
- Password y OTP se guardan con bcrypt, no en texto plano.
- `solicitarOtp()` no genera OTP usable si la persona no tiene email ni telefono; audita el caso `sin-canal`.
- `validarOtp()` usa mensaje externo uniforme para fallas de OTP y conserva el motivo especifico solo en auditoria.
- `PortalService` expone vista de cliente y vista de propietario.
- Las consultas y descargas de propietario filtran `activo_propietario.estado = 'ACTIVO'`, respetando la baja logica de V22.
- Las descargas validan `visible_portal`, estado activo y pertenencia a persona/operacion/activo permitido.
- `TenantContext` toma el tenant desde `PortalSesion` cuando no hay login administrativo, manteniendo RLS por tenant.

## Observaciones cerradas

- Obs 1: `loginPassword()` ya no revela el caso de persona elegible sin credencial.
- Obs 2: el portal ya incluye contenido para propietarios: activos, operaciones, liquidaciones y documentos.
- Obs 3: no se genera OTP sin canal de entrega.
- Obs 4: las consultas de propietario ya filtran `activo_propietario.estado = 'ACTIVO'`.
- Obs 5: la validacion OTP ya no diferencia documento inexistente de persona elegible sin OTP vigente.

## Verificacion

```text
mvn -q -pl sginmo-web -am clean package
EXIT 0
```
