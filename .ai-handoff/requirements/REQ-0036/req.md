# REQ-0036 - Multiempresa F4: aislamiento por tenant en los services

**Estado:** implementado; `mvn package` verde (2026-07-09)

## Objetivo Funcional
Que cada usuario vea/opere SOLO su tenant (defensa en la capa de servicio; la RLS de F5 es la
segunda capa). Mecanismo: NUEVO `TenantContext` (@SessionScoped, inyectable en services) =
`actual()` del usuario logueado (SUPERADMIN = -1) + `esSuperadmin()`.

- **Lecturas de catalogo** filtran `tenant IN (-1, :actual)`: CatalogoService (todos los combos:
  opciones/impuestos/formas/articulos/monedaLocal + resolver idOpcion), ListaService (ABM de
  entidad: listas/opcionesDe), GeografiaService (arbol INE -1 + ubicaciones del tenant),
  ArticuloService (grilla; catalogo con globales).
- **Lecturas transaccionales** filtran `tenant = :actual`: ActivoService (grilla + autocompletes),
  OperacionService (grilla), IngresoEgreso/Liquidacion/Caja (ya recibian el tenant del contexto).
- **Cartera de personas por tenant**: PersonaService.contar/listar muestran solo personas con
  PersonaRol o PersonaEmpresa en el tenant (la identidad es global; excluye el sentinel -1).
- **Escrituras por id validan pertenencia** antes de operar: ArticuloService (guardar/cambiarEstado),
  ActivoService (guardar), GeografiaService (guardar), ListaService (guardar/cambiarEstado),
  OperacionService (regenerarCuotas/renovar/finalizar). El alta toma el tenant del usuario; en
  edicion el registro debe ser propio (o -1 solo SUPERADMIN) y su tenant no cambia.

## Criterios De Aceptacion
- [x] TenantContext resuelve el tenant del usuario y esSuperadmin().
- [x] Catalogos leen IN(-1,:t); transaccional lee =:t; cartera de personas por tenant.
- [x] Escrituras por id validan pertenencia (propio, o -1 solo SUPERADMIN).
- [x] WAR completo empaqueta verde (mvn package EXIT 0).

## Bloqueo Formal Documentado
EmpresaService.listar (ABM de empresas) es intrinsecamente pantalla SUPERADMIN/soporte y su
aislamiento/selector se define en F6 (REQ-0038). La RLS (defensa en profundidad a nivel BD) es
F5 (REQ-0037). No se aplica V26 ni deploya: va como unidad con F2/F3.
