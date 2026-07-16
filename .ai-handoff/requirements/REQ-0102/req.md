# REQ-0102 - Portal login: acceso por documento+password sin exponer empresas + selector multi-empresa

**Numero:** REQ-0102
**Fecha de creacion:** 2026-07-16
**Estado inicial:** NUEVO
**Prioridad:** alta (seguridad)

## Texto Original
"Al tener el combo, un usuario cualquiera puede ver todo el listado de empresas configurado en el sistema
(seguridad). Que pida solo nro de documento y password; una vez adentro, poder seleccionar con un combo
arriba si el documento es cliente de mas de una empresa. Si es de una sola, todo transparente."

## Objetivo Funcional
No exponer la lista de empresas en el login publico. El acceso es por documento + password; el sistema
resuelve la(s) empresa(s) del socio. Si pertenece a mas de una, un selector en el header permite cambiar;
si es una sola, es transparente.

## Alcance
- Se quita el combo de empresa del login (portal/login.xhtml).
- PortalAuthService.loginPasswordMulti(doc, pass): busca en persona_portal_credencial de TODAS las empresas
  donde el documento es elegible (CLIENTE/PROPIETARIO) y la clave coincide (RLS superadmin app.tenant=-1).
  Devuelve un Acceso por empresa. Bloqueo por intentos y auditoria por (tenant,persona) como antes.
- OTP/primer-ingreso por documento: solicitarOtpDoc/validarOtpDoc (itera las empresas elegibles).
  definirPasswordDoc fija la MISMA clave para todas las empresas del socio (identidad unica de acceso).
- PortalSesion: guarda los accesos; autenticarMulti activa la primera; cambiarEmpresa cambia entre las
  ya autenticadas por clave; isMultiEmpresa/getAccesos/getEmpresaActual.
- WEB-INF/portal.xhtml: selector de empresa en el header (rendered si multiEmpresa) -> PortalLoginBean.cambiarEmpresa (ajax + redirect a inicio).

## Criterios De Aceptacion
- [x] El login NO muestra la lista de empresas (solo documento + password).
- [x] El acceso funciona resolviendo la empresa automaticamente.
- [x] Socio de UNA empresa: no ve selector (transparente).
- [x] Socio de VARIAS empresas: ve un selector arriba y puede cambiar; solo entre las que su clave abre.
- [x] Aislamiento por persona/empresa intacto; no se puede saltar a datos de otra empresa no autorizada.
- [x] OTP/primer-ingreso por documento; la clave definida sirve para todas las empresas del socio.

## Dependencias
- Base: PortalAuthService/PortalSesion/PortalLoginBean (REQ-0078), persona (global) + persona_portal_credencial (por tenant).
