# REQ-0055 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0055
- Tipo de cambio: BD (perfil + tabla + columna) + backend + UI + seguridad (login/routing)
- Riesgo: ALTO (portal expuesto a clientes; aislamiento de datos por persona+tenant; se toco el login)
- Archivos clave:
  - `V36__portal_cuenta.sql`: perfil PORTAL (CHECK), `documento_adjunto.visible_portal`, tabla `portal_acceso` (auditoria) con RLS.
  - `servicio/PortalService.java`: @AislarTenant; consultas SIEMPRE filtradas por `operacion.cliente=:persona` / `cobro.persona=:persona`; resumen, cuotas, pagos, documentos (visible_portal), descarga protegida, registrarAcceso.
  - `web/PortalBean.java`: toma la persona del usuario logueado (`sesion.getUsuario().getPersona()`), no de parametros; verificarAcceso por perfil PORTAL; descarga StreamedContent perezosa; audita ACCESO.
  - `webapp/WEB-INF/portal.xhtml` + `webapp/portal/inicio.xhtml`: template responsivo (sin menu admin) + pagina de cuenta.
  - `onesystem-security/.../LoginBean.java`: rutea perfil PORTAL a `/portal/inicio` (resto sin cambios).
  - `web/InicioBean.java` + `webapp/index.xhtml`: guard que saca a un usuario PORTAL del panel admin.
- Comandos probados:
  - `mvn -q clean package` (multi-modulo con onesystem-security): BUILD OK.
  - V36 en `BEGIN...ROLLBACK`: perfil PORTAL en el CHECK, columna visible_portal, tabla portal_acceso + 4 politicas RLS.
  - Deploy + Flyway V36 `success=t`.
  - `curl /portal/inicio.xhtml` sin sesion -> HTTP 200 (redirige a login; el facelet compila).
  - `python tools/smoke-test-vps.py`: 22/22 RENDER OK -> el login ADMIN sigue intacto tras tocar LoginBean.
- Cambios de datos: si, V36 (perfil, columna, tabla auditoria). Sin tocar datos existentes.
- Cambios de entorno: reutiliza `SGINMO_ARCHIVOS_DIR` (descarga de adjuntos).
- Impacto LLM/tokens: no.
- Decision esperada: revisar seguridad (aislamiento de datos + cambio de login) y limitaciones.
- Notas para auditor (SEGURIDAD):
  - Aislamiento: la persona se toma del usuario autenticado en el bean (no de la URL/parametros); el servicio filtra por esa persona Y corre @AislarTenant (RLS por tenant). Doble barrera. Revisar que ninguna consulta omita el filtro por persona.
  - Descarga: `descargar()` re-verifica visible_portal + pertenencia (persona/operaciones) antes de leer el archivo, y audita.
  - Login: solo se agrego una rama PORTAL; ADMIN/USUARIO/SUPERADMIN rutean como antes (smoke lo confirma).
  - Guards: PortalBean saca a un admin del portal (->/index); InicioBean saca a un PORTAL del panel (->/portal). El filtro global sigue exigiendo login.

## Resumen Funcional

Portal responsivo de autoservicio para clientes (perfil PORTAL): al iniciar sesion ven su estado de
cuenta (deuda vencida, proxima cuota, total pagado), sus cuotas, pagos y documentos habilitados, y pueden
descargarlos. Sin acceso al sistema administrativo ni acciones de cobro/anulacion.

## Resumen Tecnico

Perfil PORTAL vinculado a una persona (columna ya existente en usuario). PortalService @AislarTenant
consulta por persona+tenant. Auditoria en portal_acceso. Template/pagina responsivos propios. Login y
guards rutean segun perfil.

## Limitaciones Conocidas (transparencia)

- Vista de PROPIETARIO (activos/liquidaciones): DIFERIDA; esta iteracion cubre la vista de CLIENTE completa.
- "Avisos" en el portal (agenda REQ-0052): diferido.
- Descarga de comprobantes/recibos PDF: depende de REQ-0058 (aun no hecho); hoy se descargan adjuntos habilitados.
- Acceso por token/invitacion por email: diferido; hoy con credenciales controladas (usuario PORTAL).
- No hay usuario PORTAL de prueba en prod, por lo que la prueba funcional del portal es manual (ver test-plan).

## Archivos Modificados

Ver Manifiesto. V36 nueva.

## Cambios De Datos

V36: perfil PORTAL (CHECK), `documento_adjunto.visible_portal`, tabla `portal_acceso` + RLS.

## Variables De Entorno

Sin cambios (reutiliza SGINMO_ARCHIVOS_DIR).

## Pruebas Ejecutadas

Build OK; V36 rollback OK; deploy + Flyway success; portal compila; smoke 22/22 (login admin intacto).

## Pruebas Manuales Sugeridas

1. Crear un usuario perfil PORTAL vinculado a una persona con operaciones; iniciar sesion -> aterriza en el portal.
2. Verificar que ve SOLO su deuda/cuotas/pagos/documentos; que no puede entrar a pantallas admin; descargar un documento habilitado.

## Riesgos Conocidos

- Portal expuesto a clientes: el control de aislamiento es critico (revisado). Ver "Limitaciones".
