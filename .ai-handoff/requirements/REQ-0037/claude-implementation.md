# Implementacion Claude - REQ-0037

## Manifiesto Minimo Para Codex
RLS multiempresa en dos capas:
- `tools/multiempresa/V28__multiempresa_rls.sql` (staged con V26/V27): `app_tenant()` (STABLE,
  lee app.tenant con missing_ok), ENABLE+FORCE RLS y 4 politicas por tabla sobre 20 tablas de
  negocio. SELECT: `tenant=-1 OR tenant=app_tenant() OR app_tenant()=-1`. INS/UPD WITH CHECK y
  UPD/DEL USING: `app_tenant()=-1 OR tenant=app_tenant()` (un tenant solo escribe lo suyo; -1
  solo SUPERADMIN). Excluidas seguridad e identidad.
- `TenantInterceptor` (@Interceptor, @Priority PLATFORM_BEFORE+300, binding @AislarTenant):
  `set_config('app.tenant', tenant.actual()::text, true)` (SET LOCAL) por tx. Aplicado con
  @AislarTenant + @Transactional (clase) a los 17 services que tocan RLS. Auto-habilitado por
  @Priority (beans.xml discovery=all).

**Archivos:** tools/multiempresa/V28__multiempresa_rls.sql (+ v28_test.sql); servicio/AislarTenant.java,
servicio/TenantInterceptor.java (nuevos); 17 services anotados.

**Comandos probados:** `BEGIN; V26; V27; V28; <prueba 2 tenants>; ROLLBACK;` via psql ON_ERROR_STOP
contra la BD real -> EXIT 0 (tenant 1 no ve activos de tenant 500; t500 solo los suyos; superadmin
todos; insert cross-tenant negado; sin app.tenant = 0 fail-closed; catalogo ve -1). Confirmado
rol sginmo: rolsuper=f, rolbypassrls=f (FORCE RLS aplica). `mvn package` -> EXIT 0.

## Nota
La validacion runtime del interceptor con la app corriendo es F7 (2 empresas), tras aplicar
V26+V27+V28. IntegracionSeguridad (login) no lleva el interceptor: lee solo parametros -1.
