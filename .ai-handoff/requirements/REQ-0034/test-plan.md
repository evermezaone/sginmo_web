# Plan de Pruebas - REQ-0034 (F2 entidades adaptadas al V26)

## Metodo
Al no poder aplicar V26 a la BD viva todavia (va como unidad con F3), la evidencia de F2 es la
**compilacion/empaquetado del WAR completo** contra el modelo V26, mas verificaciones estaticas
de que la capa Java/JSF ya no referencia el esquema viejo.

## Evidencia
| Check | Resultado |
|---|---|
| `mvn -pl sginmo-web -am -DskipTests package` (WAR completo) | **BUILD EXIT 0** |
| Pares `*_lista/*_codigo` en entidades JPA | 0 (grep) |
| Columnas `empresa` en entidades (fuera de documento, que es F3/native) | 0 (renombradas a tenant) |
| `EntidadId` (PK compuesta) | eliminado; Entidad con `@Id @GeneratedValue Long` |
| Entidad `PersonaEmpresa` | creada y auto-escaneada (persistence.xml) |
| Bindings JSF a propiedades removidas | 0 (grep sobre webapp; los matches restantes son campos propios de Activo/Sucursal/Articulo) |
| Resolver id<->codigo (CatalogoService) | idOpcion/codigoOpcion/descripcionOpcion; reglas por codigo (loteamiento, rol EMPRESA) preservadas |
| ABMs personas/empresas | atados a persona_empresa por tenant (upsert (persona,tenant); empresa = su propio tenant) |

## Pendiente (fuera de F2)
- Verificacion funcional en runtime: requiere aplicar V26, que va junto con F3 (REQ-0035).
- Aislamiento por tenant real (filtros tenant IN(-1,:t) / =:t, pertenencia por id): F4 (REQ-0036).
- RLS: F5 (REQ-0037).
