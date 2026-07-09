# REQ-0037 - Multiempresa F5: Row-Level Security (RLS)

**Estado:** implementado; V28 verificada por rollback + `mvn package` verde (2026-07-09)

## Objetivo Funcional
Defensa en profundidad a nivel BD: aunque un service olvide un filtro, PostgreSQL no deja
ver/tocar datos de otro tenant. Dos partes:
- **V28 (migracion, staged con V26/V27):** funcion `app_tenant()` =
  `NULLIF(current_setting('app.tenant', true), '')::bigint`; `ENABLE` + `FORCE ROW LEVEL
  SECURITY` + 4 politicas (SELECT/INSERT/UPDATE/DELETE) en las 20 tablas de negocio con tenant.
  SELECT ve `-1` (global) + tenant propio; SUPERADMIN (`app_tenant()=-1`) ve todo. Escritura
  solo del tenant propio (los `-1` solo SUPERADMIN). Sin `app.tenant` -> fail-closed (transaccional).
  Excluidas las tablas de seguridad (usuario/grupo/permisos: el login las lee antes de tener
  tenant; su aislamiento es de la capa app/F6) y persona/fisica/juridica (identidad global sin tenant).
- **Interceptor (Java):** `@AislarTenant` + `TenantInterceptor` fija `app.tenant` con
  `SELECT set_config('app.tenant', <tenant>, true)` (SET LOCAL, tx-scoped, seguro con pool) al
  inicio de cada metodo, dentro de la tx de `@Transactional`. Aplicado a los 17 services que
  tocan tablas con RLS.

## Criterios De Aceptacion
- [x] V28 crea las politicas sobre el esquema V26 sin error.
- [x] Aislamiento real probado (2 tenants): cada uno ve/escribe solo lo suyo; SUPERADMIN todo;
      insert cross-tenant negado; sin app.tenant = fail-closed; catalogos ven los globales -1.
- [x] El rol de la app (sginmo) no es superuser ni bypassrls (FORCE RLS aplica).
- [x] Interceptor set_config por tx; WAR completo empaqueta verde.

## Bloqueo Formal Documentado
V26+V27+V28 + capa Java se despliegan como UNIDAD (no se aplica a la BD viva). La validacion
runtime del interceptor (que la app funcione con RLS activa, sin fail-closed indebido) es la
verificacion integral con 2 empresas: F7 (REQ-0039), tras el deploy.
