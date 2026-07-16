# REQ-0102 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-16 09:50 -03:00
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Hallazgos

### Bloqueantes

- Ninguno.

### No Bloqueantes

- El mensaje generico de `PortalAuthService` aun dice "Verifique la empresa y el documento"; conviene ajustarlo luego a "documento y contrasena" para coherencia UX, pero no expone la lista de empresas ni afecta la seguridad del flujo.

## Riesgos

Ninguno identificado.

## Pruebas Revisadas

- [x] Revision estatica de `portal/login.xhtml`: no hay combo/lista de empresas; solo documento + contrasena.
- [x] Revision de `PortalAuthService.loginPasswordMulti`: busca credenciales por documento en tenants elegibles bajo RLS superadmin (`app.tenant=-1`) y solo devuelve accesos donde bcrypt coincide.
- [x] Revision de `PortalSesion`: guarda `accesos` autenticados y `cambiarEmpresa` solo cambia si el tenant destino existe en esa lista.
- [x] Revision de `WEB-INF/portal.xhtml`: selector de empresa solo se renderiza con `portalSesion.multiEmpresa` y sus opciones salen de `portalSesion.accesos`.
- [x] Revision de OTP/primer ingreso: `solicitarOtpDoc`, `validarOtpDoc` y `definirPasswordDoc` operan por documento sin exponer empresas; `persona.numero_documento` es UNIQUE global en V1.
- [x] Revision de credenciales/RLS: `persona_portal_credencial` es por `(tenant, persona)` con RLS y politicas compatibles con `app.tenant=-1`.
- [x] Build local: `mvn -q -f Desarrollo\pom.xml -pl sginmo-web -am clean package` EXIT 0.

## Pruebas Faltantes

- [ ] Prueba manual con documento de una empresa y documento multiempresa real.
