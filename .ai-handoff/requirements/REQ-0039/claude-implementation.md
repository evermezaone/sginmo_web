# Implementacion Claude - REQ-0039

## Manifiesto Minimo Para Codex
Verificacion integral multiempresa. Artefacto ejecutable: `tools/multiempresa/f7_integracion_test.sql`.
Provisiona 2 empresas reproduciendo EmpresaService.altaEmpresa (persona + persona_juridica +
persona_rol EMPRESA en tenant -1 + sucursal tenant=empresa + usuario ADMINISTRADOR tenant=empresa)
y un catalogo (entidad) con opcion global -1 y una propia por tenant; luego valida el aislamiento
con set_config('app.tenant',...) para cada actor.

**Como reproducir (no persiste):**
  { echo "BEGIN;"; cat V26 V27 V28 f7_integracion_test.sql; echo "ROLLBACK;"; } \
    | psql -h localhost -U sginmo -d sginmo -v ON_ERROR_STOP=1 -f -
Conectar como `sginmo` es esencial: es el rol real de la app (no superuser, no bypassrls) y V28
declara FORCE ROW LEVEL SECURITY, asi que las politicas aplican incluso al owner. EXIT 0 = todo verde.

**Cobertura:** SELECT/INSERT/UPDATE cross-tenant + visibilidad de catalogo (-1 + propio) +
vision total del SUPERADMIN + "operar como". La sentinel persona_juridica(-1) (destino del FK del
rol EMPRESA) la crea V26 Fase A (lineas 11-14), verificado en la corrida.

**Capa service:** el aislamiento de usuario/grupo (tablas EXCLUIDAS de RLS) lo dan las guardas
de F4/F6 (actorTenant/tenant del contexto en lecturas y escrituras) + build del WAR verde.

## Comandos probados
- `{ echo "BEGIN;"; cat V26 V27 V28 f7_integracion_test.sql; echo "ROLLBACK;"; } | psql -h localhost
  -U sginmo -d sginmo -v ON_ERROR_STOP=1 -f -` -> **EXIT 0** (6 asserts verdes, ROLLBACK, BD viva intacta).
- `psql ... -c "SELECT rolsuper, rolbypassrls FROM pg_roles WHERE rolname='sginmo'"` -> f, f (FORCE RLS aplica).
- `psql ... -c "SELECT max(version) FROM flyway_schema_history"` -> 25 (V26/V27/V28 siguen staged).

## Nota
Confirmado en la BD real: rol app sginmo rolsuper=f, rolbypassrls=f; BD viva en flyway V25
(V26/V27/V28 siguen staged en tools/multiempresa/, se aplican como unidad en el deploy).
