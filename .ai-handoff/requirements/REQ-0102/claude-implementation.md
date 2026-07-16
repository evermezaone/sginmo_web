# REQ-0102 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-16
**Rama:** multiempresa

## Manifiesto Minimo Para Codex
- REQ: REQ-0102
- Tipo de cambio: backend (auth) + UI + sesion
- Riesgo: medio (toca autenticacion del portal; area sensible)
- Archivos clave:
  - `servicio/PortalAuthService.java`: loginPasswordMulti/solicitarOtpDoc/validarOtpDoc/definirPasswordDoc/tenantsElegibles + clase Acceso. Reusa buscarElegible/registrarFallo/identidadDe/BCrypt. RLS superadmin (fijarTenant(-1)) para buscar credenciales cross-empresa; persona es global (numero_documento UNIQUE).
  - `web/PortalSesion.java`: accesos + autenticarMulti/cambiarEmpresa/isMultiEmpresa/getAccesos/getEmpresaActual; solicitud por documento.
  - `web/PortalLoginBean.java`: ingresar->loginPasswordMulti; enviarCodigo/reenviar/validar->*Doc; guardarClave->definirPasswordDoc+loginPasswordMulti; cambiarEmpresa; se quito empresas/tenant.
  - `webapp/portal/login.xhtml`: se quita el combo de empresa.
  - `webapp/WEB-INF/portal.xhtml`: selector de empresa en el header (rendered si multiEmpresa) + xmlns:p.
- Comandos probados:
  - `python xml.dom.minidom.parse` login/portal.xhtml: OK.
  - `powershell -File tools/deploy-vps.ps1`: Build OK / Redeploy OK.
  - `python tools/smoke-test-vps.py`: TODAS OK.
- Cambios de datos: no (usa persona_portal_credencial existente) | entorno: no
- Decision esperada: aprobar
- Notas para auditor: la clave se comparte entre las empresas del socio (definirPasswordDoc). loginPasswordMulti
  solo devuelve accesos donde la clave coincide (si difieren por empresa, solo entra a las que abre). El bloqueo
  por intentos sigue por (tenant,persona). No se expone ninguna lista de empresas al usuario anonimo.

## Resumen Funcional
El socio entra con documento + password. No ve la lista de empresas. Si su documento pertenece a mas de una
empresa (y la clave abre varias), un selector arriba le permite cambiar de empresa; si es una sola, no hay selector.

## Resumen Tecnico
loginPasswordMulti busca credenciales del documento en todas las empresas (RLS superadmin), valida BCrypt por
empresa y arma un Acceso (empresa + Identidad) por cada match. La sesion guarda la lista y activa la primera;
cambiarEmpresa alterna entre las ya autenticadas. OTP/primer-ingreso y definicion de clave operan por documento
sobre todas las empresas elegibles.

## Archivos Modificados
| Archivo | Cambio |
|---|---|
| `servicio/PortalAuthService.java` | loginPasswordMulti + OTP/clave por documento + Acceso |
| `web/PortalSesion.java` | accesos + multi-empresa + solicitud por documento |
| `web/PortalLoginBean.java` | login/otp/clave por documento + cambiarEmpresa; sin combo |
| `webapp/portal/login.xhtml` | se quita el combo de empresa |
| `webapp/WEB-INF/portal.xhtml` | selector de empresa en header + xmlns:p |

## Cambios De Datos
Sin cambios (usa persona_portal_credencial y persona existentes).
## Variables De Entorno
Sin cambios.
## Pruebas Ejecutadas
- login.xhtml/portal.xhtml XML OK; Build OK; deploy Redeploy OK; smoke 37/37.
## Pruebas Manuales Sugeridas
1. Login con documento de UNA empresa -> entra sin selector.
2. Login con documento de VARIAS empresas -> selector arriba; cambiar empresa recarga la cuenta de esa empresa.
3. Primer ingreso por documento -> OTP -> definir clave -> entra; la clave sirve en todas sus empresas.
4. Verificar que el login ya NO lista empresas.
## Riesgos Conocidos
Area de autenticacion; no cubierta por el smoke (requiere OTP/credenciales). Verificacion manual necesaria.
