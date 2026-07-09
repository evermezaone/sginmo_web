# Plan de Pruebas - REQ-0038 (F6)

## Metodo
Capa Java + JSF sobre el esquema V26 (CERRADO) y RLS V28 (probado en F5). Evidencia:
build del WAR completo + buena formacion XML de los xhtml + analisis de aislamiento.
El render y el aislamiento runtime con 2 empresas se validan en F7 (REQ-0039).

## Evidencia
| Item | Resultado |
|---|---|
| mvn -pl sginmo-web -am package (WAR completo) | EXIT 0 |
| empresas.xhtml / plantilla.xhtml XML bien formado | OK |
| altaEmpresa unidad (pj+rol+sucursal+admin) | implementado; bateria SQL alta_empresa_test.sql lista para F7 |
| lecturas usuarios/grupos por tenant | filtros IN(-1,:t)/=:t con param del contexto |
| escrituras usuarios/grupos por tenant (guardar/estado/desbloq/asignaciones) | guardas actorTenant |
| selector de soporte (operar como) | TenantContext override + SuperadminBean + selector |

## Pendiente (F7)
- Login real de 2 empresas + SUPERADMIN; cada ADMIN ve/opera solo su tenant; SUPERADMIN todo;
  "operar como" acota el sistema; cruces por id negados en service Y en RLS.
