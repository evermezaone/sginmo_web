# Plan de Pruebas - REQ-0036 (F4 aislamiento en services)

## Metodo
No se puede probar en runtime todavia (V26 no aplicada; va con F2/F3 al deploy). Evidencia:
el WAR completo empaqueta verde con los filtros por tenant y la validacion de pertenencia, y
revision de que cada lectura/escritura contempla el tenant.

## Evidencia
| Check | Resultado |
|---|---|
| mvn -pl sginmo-web -am package | BUILD EXIT 0 |
| Catalogos leen IN(-1,:t) | CatalogoService/ListaService/GeografiaService/Articulo grilla |
| Transaccional lee =:t | ActivoService, OperacionService |
| Cartera de personas por tenant | PersonaService (EXISTS PersonaRol/PersonaEmpresa; excluye -1) |
| Pertenencia en escrituras por id | Articulo/Activo/Geografia/Lista/Operacion |

## Pendiente (fuera de F4)
- RLS a nivel BD: F5 (REQ-0037).
- EmpresaService.listar / selector de tenant de soporte: F6 (REQ-0038).
- Verificacion integral con 2 empresas: F7 (REQ-0039), tras aplicar V26+V27.
