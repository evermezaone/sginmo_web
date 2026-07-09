# Codex Review - REQ-0036

Estado: REQUIERE_CAMBIOS
Fecha: 2026-07-09T18:10:02-03:00
Auditor: codex

## Observaciones bloqueantes

### Obs 249 - OperacionService permite seleccionar/usar activos de otro tenant al crear operaciones

REQ-0036 exige que las lecturas transaccionales filtren `tenant = :actual` y que las escrituras por id validen pertenencia. `OperacionService` no cumple eso en la creacion de operaciones:

- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/OperacionService.java:98-103`: `activosLibres()` lista activos `LIBRE` por nombre, pero no filtra `a.tenant = :t`.
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/OperacionService.java:111-115`: `crear()` hace `em.find(Activo.class, op.getActivo())` y solo valida existencia/estado, no que `activo.tenant == tenant.actual()`.
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/OperacionService.java:242-254`: `validar()` solo exige que `op.tenant` no sea null, pero no fuerza ni compara `op.tenant` contra `tenant.actual()`.

Impacto: antes de RLS/F5 o ante una llamada directa al service, un usuario puede crear una operacion usando un activo de otra empresa, o mandar un `op.tenant` manipulado. Eso rompe el aislamiento de F4 y puede crear documentos/movimientos bajo un tenant distinto del usuario efectivo.

Solucion esperada: `activosLibres()` debe filtrar por `a.tenant = :t`; `crear()` debe fijar `op.tenant = tenant.actual()` desde el servicio, validar sucursal del mismo tenant, y abortar si el activo no pertenece a `tenant.actual()`.

### Obs 250 - PersonaService filtra la grilla, pero combos/roles/estado por id no validan tenant

La grilla de personas usa `CARTERA` por tenant, pero metodos auxiliares y escrituras por id quedan fuera del aislamiento:

- `PersonaService.java:80-88`: `porRol()` devuelve personas activas con el rol, pero el subquery no filtra `r.tenant = :t`; combos de cliente/proveedor/propietario pueden mostrar personas de otros tenants.
- `PersonaService.java:97-99`: `rolesDe(personaId)` lista roles activos de la persona sin `tenant = :t`.
- `PersonaService.java:195-199`: `cambiarEstado(id, estado)` hace `em.find(Persona.class, id)` y cambia estado global sin validar que la persona pertenezca al tenant.
- `PersonaService.java:205-235`: `agregarRol()` y `quitarRol()` no verifican que la persona/rol pertenezcan al tenant actual; `quitarRol()` permite inactivar un `PersonaRol` de otro tenant por id.

Impacto: un usuario puede ver personas/roles de otra empresa en combos o modificar la identidad/roles de personas fuera de su cartera si conoce el id. Como `persona` es identidad global compartida, este punto necesita una guarda explicita por `PersonaRol`/`PersonaEmpresa` del tenant antes de editar o mostrar detalle.

Solucion esperada: centralizar una validacion `personaVisible/editable(personaId)` basada en `PersonaRol` o `PersonaEmpresa` con `tenant.actual()`. Usarla en `porRol`, `rolesDe`, `cambiarEstado`, `agregarRol`, `quitarRol`, y en busquedas/detalles por id. `porRol` debe filtrar `r.tenant = :t`.

### Obs 251 - Acciones por id en Activo/Geografia/Articulo omiten pertenencia y permiten tocar registros de otro tenant

Hay metodos que operan por id sin la misma guarda de tenant que los `guardar()` principales:

- `ActivoService.java:255-302`: `generarLotes()` busca el contenedor con `em.find()` y crea lotes con `padre.getTenant()` sin validar que el padre sea del tenant actual.
- `ActivoService.java:319-345`: `agregarPropietario()` y `quitarPropietario()` no validan que el activo o el `ActivoPropietario` pertenezcan al tenant actual; tampoco validan que el propietario pertenezca a la cartera del tenant.
- `GeografiaService.java:107-111`: `cambiarEstado()` cambia estado de cualquier ubicacion por id sin aplicar la regla ya usada en `guardar()` (`propio` o global `-1` solo SUPERADMIN).
- `ArticuloService.java:217-252`: `copiarPropiedades()` y `listarPropiedades()` no verifican pertenencia/visibilidad del articulo origen/destino.
- `ArticuloService.java:256-292`: `agregarPropiedad()` y `eliminarPropiedad()` operan por `articuloId`/`propiedadId` sin validar que el articulo sea editable por el tenant.

Impacto: aunque las grillas principales esten filtradas, una llamada manipulada puede generar lotes, tocar propietarios, cambiar estados de geografia o modificar propiedades de articulos de otro tenant/global. Esto contradice el criterio de aceptacion de F4: escrituras por id validan pertenencia.

Solucion esperada: agregar helpers de pertenencia por entidad y usarlos en todos los metodos por id, no solo en `guardar()`. Para catalogos globales `-1`, permitir edicion solo a SUPERADMIN; para transaccionales, exigir `tenant == tenant.actual()`.

## Verificacion realizada

- Leido `req.md`, `claude-implementation.md`, `preaudit-checklist.md` y `test-plan.md`.
- Inspeccionados `TenantContext`, `CatalogoService`, `ListaService`, `ArticuloService`, `ActivoService`, `GeografiaService`, `PersonaService` y `OperacionService`.
- Comparado contra los criterios de REQ-0036: lecturas catalogo `IN(-1,:t)`, transaccional `=:t`, cartera de personas por tenant y pertenencia por id en escrituras.

No se ejecuta build como criterio de aprobacion porque el REQ queda rechazado por brechas de aislamiento en services.

---

## Reauditoria - 2026-07-09T18:42:40-03:00

Estado: REQUIERE_CAMBIOS

### Obs 249 - Cerrada

Verificado en `OperacionService`:

- `activosLibres()` ahora filtra `a.tenant = :t`.
- `crear()` fija `op.setTenant(tenant.actual())` antes de validar.
- `crear()` valida que el activo pertenezca al tenant actual.
- `crear()` valida que la sucursal pertenezca al mismo tenant.

### Obs 250 - Parcialmente cerrada; queda Obs 252

Verificado:

- `porRol()` ahora filtra `PersonaRol.tenant = :t`.
- `rolesDe()` filtra por tenant para no superadmin.
- `cambiarEstado()` usa `perteneceAlTenant(id)`.
- `quitarRol()` valida que el `PersonaRol` no sea de otro tenant.

Pero `agregarRol(personaId, rolCodigo)` sigue sin validar que `personaId` pertenezca a la cartera del tenant actual antes de crear el nuevo `PersonaRol`. El metodo solo busca duplicados en el tenant actual y luego hace:

- `PersonaService.java:239`: `rol.setPersona(personaId)`
- `PersonaService.java:241`: `rol.setTenant(tenant.actual())`

Impacto: conociendo el id de una persona ajena, un usuario puede vincularla a su tenant agregandole un rol, sin pasar por un alta/edicion controlada ni por una validacion de cartera. Esto rompe el criterio de "escrituras por id validan pertenencia".

Solucion esperada: antes de crear/reactivar el rol, exigir que la persona exista y pertenezca al tenant actual, o definir explicitamente una operacion de "importar/vincular persona global a mi tenant" con permisos/reglas propias. Si el flujo deseado es vincular una identidad global existente a un tenant, no debe quedar camuflado como simple `agregarRol` sin validacion ni permiso especifico.

### Obs 251 - Parcialmente cerrada; queda Obs 253

Verificado:

- `ActivoService.generarLotes()` valida que el contenedor sea del tenant actual.
- `ActivoService.quitarPropietario()` valida el activo de la relacion.
- `GeografiaService.cambiarEstado()` aplica la regla propio/global solo SUPERADMIN.
- `ArticuloService` agrego guardas de articulo visible/editable para propiedades.

Pero `ActivoService.agregarPropietario(activoId, propietarioId)` solo valida el tenant del activo (`exigirActivoDelTenant(activoId)`) y no valida que `propietarioId` pertenezca a la cartera del tenant actual. Luego persiste directamente:

- `ActivoService.java:352`: `ap.setPropietario(propietarioId)`

Impacto: un usuario puede asociar como propietario de un activo propio a una persona que no pertenece a su cartera/tenant si conoce el id. Eso expone o contamina relaciones de negocio entre tenants.

Solucion esperada: validar que `propietarioId` sea una persona visible/editable en la cartera del tenant actual, idealmente por `PersonaRol`/`PersonaEmpresa` con rol `PROPIETARIO` activo en `tenant.actual()`. Si se permite asociar una identidad global aun no vinculada, debe hacerse mediante un flujo explicito de vinculacion al tenant, no desde `agregarPropietario`.

No se ejecuta build como criterio de aprobacion porque el REQ sigue rechazado por guardas de pertenencia incompletas en escrituras por id.

---

## Reauditoria - 2026-07-09T18:52:42-03:00

Estado: REQUIERE_CAMBIOS

### Obs 252 - Cerrada

Verificado en `PersonaService.agregarRol`: antes de crear/reactivar el rol ahora exige `perteneceAlTenant(personaId)`, por lo que ya no vincula una identidad ajena al tenant mediante un rol por id.

### Obs 253 - Cerrada

Verificado en `ActivoService.agregarPropietario`: ahora valida que el propietario tenga rol `PROPIETARIO` activo en el tenant actual antes de persistir `ActivoPropietario`.

### Obs 254 - Lecturas/detalles por id siguen sin aislamiento de tenant

REQ-0036 no solo exige no operar otro tenant; el objetivo funcional dice que cada usuario debe ver/operar solo su tenant. Aunque las grillas principales filtran, varios metodos de lectura por id siguen resolviendo registros sin validar tenant:

- `OperacionService.java:81`: `porId(Long id)` retorna `em.find(Operacion.class, id)` sin validar `operacion.tenant = tenant.actual()`.
- `OperacionService.java:83-87`: `cuotasDe(Long operacionId)` lista cuotas por `operacion = :op` sin verificar que la operacion sea del tenant actual.
- `OperacionService.java:91-94`: `moraDe(Long cuotaId)` llama `f_mora_cuota` por id de cuota sin validar la operacion/tenant de esa cuota.
- `PersonaService.java:102-105`: `buscar`, `fisicaDe` y `juridicaDe` hacen `em.find` por id sin validar `perteneceAlTenant(id)`.
- `ActivoService.java:99`: `buscar(Long id)` retorna cualquier activo por id; lo usa `ActivoConverter`.
- `ActivoService.java:167-179`: `propietariosDe` y `propietariosConId` listan propietarios de cualquier `activoId` sin exigir `exigirActivoDelTenant(activoId)`.
- `GeografiaService.java:64-65`: `buscarPorId(Long id)` retorna cualquier ubicacion por id, sin aplicar la regla visible `(tenant=-1 OR tenant=:actual)`.

Impacto: con una URL/request/converter manipulado o un id conocido, un usuario puede ver detalle, cuotas, mora, propietarios o datos de personas/activos/ubicaciones de otro tenant aunque la grilla inicial este filtrada. F5/RLS lo reforzara despues, pero F4 es la defensa de service y debe cerrar estos accesos.

Solucion esperada: aplicar helpers de visibilidad/pertenencia tambien en lecturas por id. Si el registro no pertenece/no es visible al tenant, devolver `null`, lista vacia o lanzar `NegocioException` segun el caso. Para cuotas/mora, validar la operacion asociada al tenant antes de devolver datos o invocar la funcion.

No se ejecuta build como criterio de aprobacion porque el REQ sigue rechazado por lecturas por id sin aislamiento.

---

## Reauditoria - 2026-07-09T19:06:45-03:00

Estado: APROBADO_POR_CODEX

### Obs 254 - Cerrada

Verificado:

- `OperacionService.porId()` ahora devuelve `null` si la operacion no pertenece al tenant actual; `cuotasDe()` reutiliza esa guarda y `moraDe()` valida la operacion asociada antes de invocar la funcion.
- `PersonaService.buscar()`, `fisicaDe()` y `juridicaDe()` ahora validan `perteneceAlTenant(personaId)` antes de devolver datos.
- `ActivoService.buscar()` ahora valida el tenant del activo; `propietariosDe()` y `propietariosConId()` devuelven vacio si el activo no es visible para el tenant actual.
- `GeografiaService.buscarPorId()` ahora aplica visibilidad por tenant actual o global `-1`, con excepcion para SUPERADMIN.

### Resultado final

Obs 249, 250, 251, 252, 253 y 254 cerradas. El REQ-0036 cumple el aislamiento por tenant requerido en grillas, combos, lecturas por id y escrituras por id revisadas.

Verificacion ejecutada:

- `mvn -q -pl sginmo-web -am clean package` - EXIT 0
