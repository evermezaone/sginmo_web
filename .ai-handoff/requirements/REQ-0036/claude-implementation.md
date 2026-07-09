# Implementacion Claude - REQ-0036

## Manifiesto Minimo Para Codex
Aislamiento por tenant en la capa de servicio (F4), sobre un nuevo `TenantContext`
(@SessionScoped en `web`, inyectado en services @ApplicationScoped via proxy) que expone
`actual()` (tenant del usuario; SUPERADMIN=-1) y `esSuperadmin()`.

Patron aplicado:
- Catalogos: `WHERE (x.tenant = -1 OR x.tenant = :t)` — CatalogoService, ListaService,
  GeografiaService, ArticuloService (grilla). idOpcion(lista,codigo) resuelve dentro de IN(-1,:t)
  prefiriendo el propio (ORDER BY tenant DESC).
- Transaccional: `WHERE x.tenant = :t` — ActivoService (+autocompletes), OperacionService.
- Cartera de personas: PersonaService filtra EXISTS(PersonaRol|PersonaEmpresa con tenant=:t) y
  excluye p.id = -1.
- Pertenencia en escrituras por id: helper que valida (propio || (-1 && SUPERADMIN)); el alta
  fuerza tenant = actual; la edicion preserva el tenant original. En ArticuloService, ActivoService,
  GeografiaService, ListaService y OperacionService (regenerar/renovar/finalizar).

**Archivos:** web/TenantContext.java (nuevo); servicio/{CatalogoService, ListaService, ArticuloService,
ActivoService, GeografiaService, PersonaService, OperacionService}.java.

**Comandos probados:** `mvn -q -pl sginmo-web -am -DskipTests package` -> EXIT 0. La verificacion
funcional real (2 empresas, cruces negados) es F7 (REQ-0039), tras aplicar V26+V27.

## Nota
La RLS (F5) es la segunda capa (a nivel BD). EmpresaService.listar (empresas) se aisla en F6.
