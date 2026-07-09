# Implementacion Claude - REQ-0034

## Manifiesto Minimo Para Codex
Adaptacion completa de la capa Java/JSF al esquema V26 (multiempresa), en rama `multiempresa`.
Se hizo por partes, cada una con build verde:
- **empresa->tenant** (Usuario + Operacion/Activo/Planilla/IngresoEgreso): campo/getter/setter,
  JPQL (X.empresa->X.tenant), y `ContextoEmpresa.getNombreEmpresa` usa razon social (el fantasia
  es per-tenant ahora). El SQL NATIVO que referencia columnas renombradas de documento/planilla/
  cobro/etc. es F3 (REQ-0035), NO se toca aca.
- **Entidad PK numerica**: `@IdClass(EntidadId)` -> `@Id @GeneratedValue Long id`, `lista`,
  `codigo`, `tenant`. `EntidadId.java` eliminado. Consumidores (CatalogoService/ListaService/
  ArticuloService/ListaBean) resueltos.
- **Refs por id**: resolver en `CatalogoService` (`idOpcion(lista,codigo)`, `codigoOpcion(id)`,
  `descripcionOpcion(id)`). Reglas por codigo resueltas: ActivoService (TIPOS_CONTENEDOR_LOTE ->
  ids, loteamiento), EmpresaService/PersonaService (rol EMPRESA por id). Combos `itemValue` -> id.
  atributo_por_tipo.tipo en la query nativa de atributosDe pasa a `apt.tipo=:id`.
- **persona_empresa**: entidad `PersonaEmpresa` (auto-escaneada). Persona/PersonaFisica/
  PersonaJuridica reducidas a identidad. `PersonaService`/`EmpresaService` cargan/upsertan
  persona_empresa por (persona, tenant); `PersonaBean`/`EmpresaBean` mantienen `datosEmpresa`
  y los ABMs (personas.xhtml 13 refs, empresas.xhtml) lo enlazan.

**Archivos:** onesystem-security/Usuario + UsuarioService/UsuarioBean; dominio.{catalogo,activo,
operacion,persona} (Entidad, Articulo, ArticuloPropiedad, UbicacionGeografica, Activo, Operacion,
Planilla, IngresoEgreso, Liquidacion, Persona, PersonaFisica, PersonaJuridica, PersonaRol, NUEVA
PersonaEmpresa); servicio.{CatalogoService, ListaService, ArticuloService, ActivoService,
CajaService, LiquidacionService, IngresoEgresoService, OperacionService, GeografiaService,
PersonaService, EmpresaService}; web.{ContextoEmpresa, ListaBean, ActivoBean, PersonaBean,
EmpresaBean, IngresoEgresoBean, OperacionBean}; webapp (listas/articulos/activos/operaciones/
ingresos-egresos/liquidaciones/geografia/personas/empresas .xhtml).

**Comandos probados:** `mvn -q -pl sginmo-web -am -DskipTests package` (WAR completo) -> **EXIT 0**.
Verificacion por grep de 0 pares *_lista/*_codigo en entidades, 0 empresa fuera de documento,
0 bindings JSF muertos.

## Nota de secuencia
NO se aplica V26 ni se deploya hasta F3. El aislamiento por tenant real (filtros, pertenencia,
RLS) es F4/F5. Esta revision es de la coherencia estructural de la capa Java/JSF con V26.
