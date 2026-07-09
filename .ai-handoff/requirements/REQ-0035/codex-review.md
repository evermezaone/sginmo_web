# Codex Review - REQ-0035

Estado: REQUIERE_CAMBIOS
Fecha: 2026-07-09T13:10:00-04:00
Auditor: codex

## Observaciones bloqueantes

### Obs 246 - f_cobrar_documento acepta codigos invalidos de dato_cobro y los guarda como NULL

V27 cambia `dato_cobro.emisor/procesador/motivo_rechazo` de pares `(lista,codigo)` a FK `bigint` contra `entidad`, pero la conversion dentro de `f_cobrar_documento` no valida que el codigo recibido exista.

En `tools/multiempresa/V27__multiempresa_sps.sql:125-131`, los valores se insertan asi:

- `emisor = (SELECT e.entidad ... e.codigo = NULLIF(p_emisor,'') ... LIMIT 1)`
- `procesador = (SELECT e.entidad ... e.codigo = NULLIF(p_procesador,'') ... LIMIT 1)`
- `motivo_rechazo = (SELECT e.entidad ... e.codigo = NULLIF(p_motivo_rechazo,'') ... LIMIT 1)`

Si el usuario/API envia un codigo no vacio pero inexistente, la subconsulta devuelve `NULL`. El cobro se registra igual y `dato_cobro` queda sin el catalogo correspondiente. Esto es una regresion respecto al esquema anterior: V1/V24 insertaban `emisor_codigo`, `procesador_codigo` y `motivo_rechazo_codigo`, y la FK compuesta de `dato_cobro` contra `entidad` rechazaba un codigo invalido (`V1__esquema_inicial.sql:533-552`, `V24__cobro_flags_completos.sql:98-107`).

Impacto: una forma de pago que exige emisor/procesador/motivo puede aceptar cualquier texto no vacio y terminar persistiendo `NULL`, perdiendo trazabilidad del medio de pago. En cobros, esto es dato sensible de conciliacion.

Solucion esperada: resolver esos ids en variables y lanzar `RAISE EXCEPTION` si el parametro viene no vacio y no existe una opcion visible al tenant. Ejemplo: si `p_emisor` no es blanco y no hay `EMISORES` para `tenant IN (-1, v_tenant)`, abortar con mensaje de negocio. Lo mismo para `PROCESADORES` y `MOTIVOS_RECHAZO`. Conviene ademas ordenar la resolucion para preferir la opcion del tenant sobre la global cuando ambos compartan codigo.

## Verificacion realizada

- Leido `req.md`, `claude-implementation.md`, `preaudit-checklist.md` y `test-plan.md`.
- Inspeccionado `tools/multiempresa/V27__multiempresa_sps.sql` y `tools/multiempresa/v27_test.sql`.
- Inspeccionado SQL nativo en `OperacionService`, `CajaService`, `ReporteService` e `InicioBean`.
- Comparado contra `V1__esquema_inicial.sql` y `V24__cobro_flags_completos.sql`.

No se ejecuta build como criterio de aprobacion porque el REQ queda rechazado por regresion de validacion en el motor.

---

## Reauditoria - 2026-07-09T13:17:00-04:00

Estado: REQUIERE_CAMBIOS

### Obs 246 - Cerrada

Verificado en `tools/multiempresa/V27__multiempresa_sps.sql`: `f_cobrar_documento` ahora resuelve `p_emisor`, `p_procesador` y `p_motivo_rechazo` en variables (`v_emisor`, `v_procesador`, `v_motivo_rechazo`), prioriza la opcion del tenant con `ORDER BY (e.tenant = v_tenant) DESC`, y hace `RAISE EXCEPTION` si el codigo no vacio no existe para `tenant IN (-1, v_tenant)`.

Tambien existe evidencia especifica en `tools/multiempresa/v27_obs246_test.sql`: codigo invalido aborta y codigo valido prefiere la opcion propia del tenant.

### Obs 247 - f_cobrar_documento no valida que la nota de credito asociada pertenezca al mismo tenant

`f_cobrar_documento` recibe `p_ntcr` como documento asociado cuando la forma de pago exige nota de credito. En V26, `documento` tiene `tenant`, y el REQ-0035 pide adaptar el motor con validacion de coherencia de tenant. La funcion valida que la NTCR exista, que sea `tipo = 'NTCR'`, que no este anulada y que sea del mismo cliente, pero no compara el tenant de esa NTCR contra `v_tenant` del documento cobrado:

- `tools/multiempresa/V27__multiempresa_sps.sql:91-100` selecciona solo `tipo, persona, estado` desde `documento WHERE documento = p_ntcr`.
- Luego inserta `p_ntcr` en `dato_cobro.ntcr_documento` (`tools/multiempresa/V27__multiempresa_sps.sql:142-150`).

Impacto: antes de F5/RLS, o ante cualquier llamada directa al motor, se puede asociar al cobro una nota de credito de otro tenant si coincide el cliente/persona. Eso rompe la coherencia multiempresa del cobro y deja una referencia cruzada entre empresas en `dato_cobro`.

Solucion esperada: leer tambien `documento.tenant` de la NTCR y hacer `RAISE EXCEPTION` si `v_ntcr_tenant <> v_tenant`. Idealmente agregar prueba rollback que cree una NTCR de otro tenant para el mismo cliente y verifique que `f_cobrar_documento` la rechaza.

No se ejecuta build como criterio de aprobacion porque el REQ vuelve a quedar rechazado por validacion incompleta de tenant en el motor.

---

## Reauditoria - 2026-07-09T17:53:14-03:00

Estado: REQUIERE_CAMBIOS

### Obs 247 - Cerrada

Verificado en `tools/multiempresa/V27__multiempresa_sps.sql`: `f_cobrar_documento` ahora lee `documento.tenant` de la NTCR en `v_ntcr_tenant` y aborta con `RAISE EXCEPTION 'La nota de crédito pertenece a otra empresa'` si difiere de `v_tenant`.

Tambien existe evidencia especifica en `tools/multiempresa/v27_obs246_test.sql`: el TEST 3 crea una NTCR de otro tenant para el mismo cliente y espera rechazo.

### Obs 248 - f_anular_cobro no prioriza el motivo propio del tenant cuando existe tambien global

V26 permite que una lista tenga el mismo `codigo` en global `tenant = -1` y en un tenant propio, porque `entidad_lista_codigo_tenant_key` es unico por `(lista, codigo, tenant)` (`tools/multiempresa/V26__multiempresa_esquema.sql:38`). Este es el mismo patron que `f_cobrar_documento` ya corrigio para `EMISORES`, `PROCESADORES` y `MOTIVOS_RECHAZO`: resolver por `tenant IN (-1, v_tenant)` pero prefiriendo la opcion propia con `ORDER BY (e.tenant = v_tenant) DESC`.

En cambio, `f_anular_cobro` resuelve `MOTIVOS_ANULACION` con:

- `tools/multiempresa/V27__multiempresa_sps.sql:185`: `WHERE e.lista = 'MOTIVOS_ANULACION' AND e.codigo = p_motivo AND e.tenant IN (-1, v_tenant) LIMIT 1`

Al no tener `ORDER BY`, si existen un motivo global y otro del tenant con el mismo codigo, PostgreSQL puede devolver cualquiera. Impacto: la anulacion puede persistir el `motivo` global aunque el tenant tenga una opcion propia configurada para ese codigo, perdiendo la configuracion de empresa y dejando un resultado no deterministico.

Solucion esperada: resolver `MOTIVOS_ANULACION` con el mismo criterio usado en cobro: `ORDER BY (e.tenant = v_tenant) DESC LIMIT 1`. Agregar una prueba rollback que cree `MOTIVOS_ANULACION` global y propio con el mismo codigo y verifique que `anulacion.motivo` queda apuntando al id del tenant.

No se ejecuta build como criterio de aprobacion porque el REQ queda rechazado por resolucion no deterministica de catalogo tenant/global en el motor.

---

## Reauditoria - 2026-07-09T18:04:34-03:00

Estado: APROBADO_POR_CODEX

### Obs 248 - Cerrada

Verificado en `tools/multiempresa/V27__multiempresa_sps.sql`: `f_anular_cobro` ahora resuelve `MOTIVOS_ANULACION` con `ORDER BY (e.tenant = v_tenant) DESC LIMIT 1`, prefiriendo la opcion propia del tenant sobre la global cuando comparten codigo.

Tambien existe evidencia especifica en `tools/multiempresa/v27_obs246_test.sql`: el TEST 4 crea `MOTIVOS_ANULACION` global y propio con codigo `ERROR`, anula un cobro del tenant y valida que `anulacion.motivo` sea el id propio del tenant (`-9802`).

### Verificacion final

- Obs 246 cerrada: codigos invalidos de `dato_cobro` abortan y los validos priorizan tenant.
- Obs 247 cerrada: NTCR asociada debe pertenecer al mismo tenant del documento cobrado.
- Obs 248 cerrada: motivo de anulacion prioriza tenant frente a global.
- Build ejecutado desde `Desarrollo`: `mvn -q -pl sginmo-web -am clean package` con `JAVA_HOME=C:\Program Files\Java\jdk-23` -> EXIT 0.

Resultado: REQ-0035 aprobado.
