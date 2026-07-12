# REQ-0055 - Plan De Implementacion

**Estado:** APROBADO_PARA_DESARROLLO
**Fecha:** 2026-07-12

## Estrategia

Perfil PORTAL (reusa la columna usuario.persona ya existente). PortalService @AislarTenant que consulta
SIEMPRE por la persona del usuario logueado (operacion.cliente / cobro.persona) + RLS por tenant. Template
y pagina responsivos propios (sin menu admin). Auditoria de accesos/descargas. Login rutea PORTAL al portal;
guards evitan cruces admin<->portal.

## Archivos A Modificar

| Archivo | Cambio |
|---|---|
| V36__portal_cuenta.sql | perfil PORTAL + visible_portal + portal_acceso + RLS |
| servicio/PortalService.java | NUEVO — consultas por persona, descarga, auditoria |
| web/PortalBean.java | NUEVO — bean del portal |
| WEB-INF/portal.xhtml + portal/inicio.xhtml | NUEVO — template + pagina responsiva |
| onesystem-security/LoginBean.java | rutea PORTAL |
| web/InicioBean.java + index.xhtml | guard anti-cruce |

## Pruebas Previstas

- [ ] Build OK (multi-modulo)
- [ ] V36 rollback (perfil, columna, tabla, RLS)
- [ ] Deploy + Flyway V36 + portal compila + smoke (login admin intacto)
- [ ] Aislamiento por persona+tenant (revision de codigo)

## Riesgos

- Seguridad: aislamiento de datos entre clientes/tenants; se toca el login (mitigado por smoke).

## Cambios De Datos

V36: perfil PORTAL, documento_adjunto.visible_portal, tabla portal_acceso + RLS.
