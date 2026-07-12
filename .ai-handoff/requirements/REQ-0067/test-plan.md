# REQ-0067 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` (multi-modulo) | Build OK; V46 y clases Auditoria* en el WAR | OK |
| T02 | Deploy + redeploy en la VPS | HTTP 200 login | OK |
| T03 | Flyway V46 | tabla + RLS + pantalla aplicadas | OK (implicito: la pantalla auditoria renderiza consultando la tabla) |
| T04 | `python tools/smoke-test-vps.py` | 31/31 render OK incl. `auditoria` | OK — TODAS OK |
| T05 | Render de auditoria ejecuta SELECT sobre auditoria_funcional bajo RLS | 200 sin 500 | OK |

## Pruebas Manuales

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | Desbloquear un usuario | fila DESBLOQUEAR visible en Auditoria | pendiente |
| M02 | Filtrar por fecha/usuario/accion/campo/entidad | resultado acotado | pendiente |
| M03 | Cambiar un campo con `registrarCambios` | una fila EDITAR con valor_anterior/nuevo | pendiente (rollout por-ABM) |
| M04 | Campo sensible (password/token) | se guarda "***" | pendiente (cubierto por codigo: esSensible) |
| M05 | Dos empresas | una no ve la auditoria de la otra | pendiente (RLS por codigo/migracion) |

## Revision Transversal (SEGURIDAD / MULTIEMPRESA)

- Auditoria inmutable a nivel BD (sin UPDATE/DELETE policies).
- Sin secretos: enmascarado por nombre de campo antes de persistir.
- Aislamiento por tenant: RLS (patron V28) + @AislarTenant.
- Permiso propio: `auditoria/VER` exigido en las lecturas.

## Datos De Prueba

Un usuario para bloquear/desbloquear (genera una fila de auditoria real).
