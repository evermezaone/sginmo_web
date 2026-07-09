# Implementacion Claude - REQ-0038

## Manifiesto Minimo Para Codex
F6 en incrementos verdes (F6a..F6e):
- **F6a** `EmpresaService.altaEmpresa` (unidad pj+rol EMPRESA en -1 + sucursal por defecto +
  usuario ADMINISTRADOR tenant=empresa). ★ Fix latente F4: `PersonaRol` no mapeaba `tenant`
  (NOT NULL en V26) aunque PersonaService.CARTERA ya lo consultaba y agregarRol lo persistia
  sin valor; se agrego el campo; el rol EMPRESA se crea en -1 (backfill V26 linea 215).
- **F6b** lecturas ABM por tenant: UsuarioService/GrupoService listar/contar/gruposActivos/
  gruposPorId reciben el tenant del contexto (SUPERADMIN -1 ve todo; ADMIN su empresa; grupos
  suman plantillas -1). `SesionUsuario.tenantActual()`. onesystem-security se aisla con su
  propia SesionUsuario (usuario/grupo EXCLUIDOS de RLS -> aislamiento de la capa app).
- **F6c** escrituras ABM por tenant: guardar/cambiarEstado/desbloquear de usuario y guardar/
  cambiarEstado de grupo reciben actorTenant (-1=SUPERADMIN) y validan pertenencia; plantillas
  -1 de solo lectura para el ADMIN; un grupo nuevo nace en el tenant del operador.
- **F6d** selector de soporte: `TenantContext` gana override (operarComo/volverAGlobal,
  esSuperadminReal, actual() override-aware, backward-compatible). `SuperadminBean` +
  selector en plantilla.xhtml (solo SUPERADMIN, recarga la pagina al cambiar). `ContextoEmpresa`
  usa tenant.actual(). **F6d-alta**: EmpresaBean.guardar enruta a altaEmpresa cuando el
  SUPERADMIN crea empresa nueva; pestana 'Administrador inicial' en empresas.xhtml.
- **F6e** guardas en agregarAGrupo/agregarPermiso (actorTenant): el usuario objetivo debe ser
  del tenant del actor y el grupo asignable (plantilla -1 o propio).

**Archivos:** sginmo-web: EmpresaService, EmpresaBean, empresas.xhtml, TenantContext,
ContextoEmpresa, SuperadminBean (nuevo), plantilla.xhtml, PersonaRol, PersonaService.
onesystem-security: UsuarioService, GrupoService, SesionUsuario, UsuarioBean, GrupoBean.

**Comandos probados:** `mvn -q -pl sginmo-web -am -DskipTests package` -> EXIT 0.
empresas.xhtml y plantilla.xhtml validados XML bien formados.

## Nota
Backward-compat del override: para un usuario NO superadmin operarComo lanza, asi que
actual() es identico a F4; solo cambia el comportamiento cuando un SUPERADMIN elige impersonar.
