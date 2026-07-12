# REQ-0055 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` (multi-modulo) | Build OK | OK |
| T02 | V36 en `BEGIN...ROLLBACK` | perfil PORTAL + visible_portal + portal_acceso + 4 RLS | OK |
| T03 | Backup previo | dump OK | OK |
| T04 | Deploy + Flyway V36 | success=t | OK |
| T05 | `curl /portal/inicio.xhtml` sin sesion | redirige a login (200); facelet compila | OK (HTTP 200) |
| T06 | `python tools/smoke-test-vps.py` | 22/22 render OK -> login ADMIN intacto tras tocar LoginBean | OK (TODAS OK) |

## Pruebas Manuales

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | Usuario PORTAL vinculado a persona con operaciones | login aterriza en /portal/inicio | pendiente (requiere alta de usuario PORTAL) |
| M02 | Cliente ve su cuenta | solo su deuda/cuotas/pagos/documentos | pendiente |
| M03 | Cliente intenta pantalla admin | expulsado a /index -> /portal (guard) | pendiente |
| M04 | Admin intenta /portal | expulsado a /index | pendiente |
| M05 | Descarga de documento habilitado | descarga + queda auditada en portal_acceso | pendiente |

## Revision Transversal (SEGURIDAD)

- Aislamiento: TODAS las consultas de PortalService filtran por `:persona` (operacion.cliente / cobro.persona);
  la persona proviene del usuario autenticado (`sesion.getUsuario().getPersona()`), NO de la URL. Ademas
  @AislarTenant fija app.tenant -> RLS por empresa. Doble barrera (persona + tenant).
- Regla "documentos privados solo si visibles para portal": la consulta exige `visible_portal=true`.
- Regla "nunca exponer datos de otros clientes/tenants": sin persona -> resultados vacios; descarga re-verifica
  pertenencia antes de leer el archivo.
- Login: solo se agrega rama PORTAL; ADMIN/USUARIO/SUPERADMIN sin cambios (smoke lo confirma).

## Datos De Prueba

Requiere crear un usuario perfil PORTAL con `persona` = un cliente que tenga operaciones/cuotas/cobros.
