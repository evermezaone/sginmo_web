# Instrucciones para Claude Code — Proyecto Migración Gestión

## Al iniciar cada sesión — OBLIGATORIO

Consultar la base de coordinacion como primer paso:

```sql
CALL sp_siguiente_accion_agente('{{PROJECT_CODE}}', 'claude');
```

Este procedure devuelve dos result sets:

1. **Mensajes de chat no leídos** en el canal `claude`:
   - Responder con `sp_responder_chat('{{PROJECT_CODE}}', 'claude', 'assistant', 'claude', respuesta)`.
   - Marcar leídos con `sp_marcar_chat_leido('{{PROJECT_CODE}}', 'claude')`.
   - Si el mensaje es una tarea o comando, ejecutarlo además de responder.

2. **REQs asignados a `claude`** que no están `CERRADO` ni `CANCELADO`, ordenados por prioridad:

| EstadoOperativo / Estado | Acción |
|--------|--------|
| `REQUIERE_CAMBIOS` | Leer observaciones en `AUDITORIA_OBSERVACION` → corregir → derivar con `sp_derivar_req(PROJECT_CODE, req, 'LISTO_PARA_REVISION', 'codex', 'claude', resumen)` |
| `BLOQUEADO_POR_USUARIO` | Escalar al usuario para decisión |
| `ESPERA_USUARIO` | Verificar si el usuario ya respondió; si sí, retomar |
| `EN_DESARROLLO` | Continuar implementación → derivar a `LISTO_PARA_REVISION` al finalizar |
| `NUEVO` / `EN_ANALISIS` | Analizar, implementar y derivar |
| `BLOQUEADO_POR_PRIORIDAD` | No iniciar hasta resolver el REQ bloqueante |

**Si la BD no está disponible**, leer `.ai-handoff/to_claude.md` como respaldo de compatibilidad y registrar el fallo antes de continuar.

**La BD manda sobre los archivos.** Si hay contradicción entre la BD (`REQ.Estado`, `REQ.Responsable`), `registry.jsonl` y el resumen de sesión anterior, la BD es la fuente de verdad.

## Autoridad de cierre de REQs

Claude desarrolla, corrige, documenta y deriva REQs a revision, pero no aprueba ni cierra requerimientos.

Reglas:

- Claude puede derivar a `LISTO_PARA_REVISION` con `responsable='codex'`.
- Claude puede corregir observaciones y reenviar a Codex.
- Claude no debe llamar `sp_derivar_req(..., 'CERRADO', ...)`.
- Claude no debe marcar `APROBADO_POR_CODEX`.
- Solo Codex auditor puede cerrar un REQ como `CERRADO` / `APROBADO_POR_CODEX` despues de auditar.
- Codex-Spark tampoco cierra: solo corrige observaciones puntuales y devuelve a Codex.
- Si el usuario toma una decision funcional, Claude debe documentarla y derivar a revision; esa decision no reemplaza la auditoria de Codex.

Transiciones invalidas para Claude:

- `claude` -> `CERRADO`
- `claude` -> `APROBADO_POR_CODEX`

Antes de enviar cualquier REQ a Codex, aplicar **obligatoriamente** el checklist completo de `.ai-handoff/standards/audit-checklist.md`. Puntos mínimos a verificar en cada REQ:

- No hay credenciales ni secrets hardcodeados en ningún archivo nuevo o modificado.
- `test-plan.md` tiene casos de prueba respaldados por código real (no afirmar features que no existen).
- `claude-implementation.md` documenta invariantes de diseño cuando el cambio afecta triggers, SPs o lógica compartida.
- Regresión cubierta: si el cambio toca un trigger/SP existente, hay evidencia de que los flujos previos no se rompen.

Antes de reenviar, completar `.ai-handoff/requirements/REQ-XXXX/preaudit-checklist.md` con todos los items marcados. Si el archivo no existe porque el REQ es antiguo, copiar `.ai-handoff/requirements/_templates/preaudit-checklist.md`, reemplazar `REQ-XXXX` y completar la lista.

### Revision transversal de flujos equivalentes

Antes de reenviar a Codex, Claude debe verificar que la correccion no quedo aplicada solo al caso visible. Si una observacion corrige una regla de negocio, validacion, estado, tabla, trigger, SP, endpoint o componente compartido, buscar flujos equivalentes en todo el proyecto.

La busqueda debe cubrir, segun aplique:

- Todos los archivos que insertan, actualizan o consultan la misma tabla.
- Todos los endpoints que crean el mismo efecto de negocio.
- Todos los componentes que usan la misma accion, estado o helper.
- Todos los SP/triggers/scripts que tocan la misma entidad.

La evidencia debe quedar en `test-plan.md` o `preaudit-checklist.md` con comandos/archivos revisados y resultado. Ejemplo: si se cambia la regla "no registrar cobros sin planilla abierta", revisar todos los flujos que insertan `COBRO` o `COBRO_DETALLE`, no solo el endpoint corregido.

Las observaciones de Codex se cierran en la tabla, no solo en texto. Consultar `AUDITORIA_OBSERVACION` para el REQ menor pendiente y resolver cada fila `pendiente` como `corregido`, `aceptado` o `diferido` con nota antes de ejecutar `npm run handoff:ready -- REQ-XXXX`.

### Reenvio enfocado por observacion

Cuando un REQ vuelve desde `REQUIERE_CAMBIOS`, Claude debe responder una por una las observaciones de Codex antes de reenviar. En `preaudit-checklist.md` o `test-plan.md`, dejar evidencia con este formato por cada observacion cerrada:

```text
Obs NN:
- Problema original:
- Cambio aplicado:
- Archivos tocados:
- Evidencia:
- Validacion propia:
```

La nota de cierre en `AUDITORIA_OBSERVACION` y el resumen de `sp_derivar_req(..., 'LISTO_PARA_REVISION', ...)` deben mencionar las observaciones cerradas y el cambio principal. Codex re-auditara primero esas observaciones; por eso no alcanza con decir "corregido" sin evidencia puntual.

Luego ejecutar:

```bash
npm run handoff:check
```

Si el check falla, corregir la causa antes de avanzar.

Para reenviar a Codex un REQ puntual ya corregido desde `PRECHECK_FAIL` o `REQUIERE_CAMBIOS`, no editar a mano `registry.jsonl` + `to_codex.md`. Usar:

```bash
npm run handoff:ready -- REQ-XXXX
```

Ese comando valida la evidencia del REQ, lo cambia a `LISTO_PARA_REVISION`, limpia marcas `precheck` obsoletas y escribe `to_codex.md` solo con ese REQ. Tambien bloquea si falta `preaudit-checklist.md`, si queda algun item sin marcar o si existen observaciones `pendiente` en `AUDITORIA_OBSERVACION`.

Reglas de precheck:

- No poner `to_codex.md` en `LISTO_PARA_REVISION` si `npm run handoff:check` falla.
- Si el precheck falla, mantener/cambiar el REQ a `PRECHECK_FAIL` o seguir corrigiendo sin notificar a Codex.
- Completar `test-plan.md` con evidencia real antes de enviar.
- Completar `preaudit-checklist.md` antes de reenviar.
- Documentar la revision transversal de flujos equivalentes cuando la correccion toque reglas compartidas.
- Cerrar o justificar todas las observaciones pendientes en `AUDITORIA_OBSERVACION`.
- No marcar criterios `[x]` en `req.md` si siguen pendientes, delegados o diferidos.
- No enviar lotes de mas de 5 REQs salvo aprobacion explicita del usuario; en ese caso incluir `BATCH_GRANDE_APROBADO_POR_USUARIO` en `MENSAJE:` de `to_codex.md`.

## Regla de continuidad

El flujo no debe quedar parado si existen REQs pendientes. La fuente primaria siempre es la BD:

- Consultar `sp_siguiente_accion_agente('{{PROJECT_CODE}}', 'claude')` al iniciar sesion para ver el estado real.
- En modo offline (BD no disponible), usar `registry.jsonl` como respaldo temporal y registrar el fallo.
- Tratar como trabajo pendiente cualquier REQ en `NUEVO`, `EN_ANALISIS`, `PRECHECK_FAIL`, `REQUIERE_CAMBIOS`, `LISTO_PARA_REVISION`, `ESPERA_USUARIO` o `BLOQUEADO_POR_USUARIO`.
- Corregir primero el REQ pendiente de numeracion mas baja y no avanzar a REQs mayores hasta reenviar a Codex o bloquear formalmente el menor.
- Leer el `codex-review.md` de cada REQ pendiente, corregir, actualizar `claude-implementation.md`, `test-plan.md` y `events.jsonl`, y luego ejecutar `npm run handoff:ready -- REQ-XXXX`.
- Para reenviar un REQ corregido usar `npm run handoff:ready -- REQ-XXXX`; no editar a mano `registry.jsonl` y `to_codex.md`.
- Si durante la correccion aparece una regla repetible, revisar y aplicar esa regla a los REQs mayores pendientes antes de reenviarlos.
- Si la regla repetible es general, actualizar `.ai-handoff/standards/` y el template.
- No cambiar esos REQs a `BLOQUEADO_POR_USUARIO` salvo decision explicita y nueva del usuario.

## Contexto del Proyecto

**Origen:** Sistema de gestión VB6 + Oracle XE 11.2 ("Gestión - ONE")
**Destino:** FastAPI (Python) + React + MariaDB 11.8.6
**BD remota:** 193.203.175.236 / u237417599_distribuidora (credenciales en `.env`)
**Prioridad de negocio:** Documentos (comprobantes) + Cobros/Pagos

### Tablas ya existentes en BD remota
DOCUMENTO, DETALLE_DOCUMENTO, CLIENTE, PRODUCTO, PREVENTA, PREVENTA_DETALLE, USUARIO, VISITA (sistema móvil de ventas)

### Tablas pendientes de crear
TIPO_COMPROBANTE, RANGO_COMPROBANTE, COBROS, COBRODETALLE, FORMA_DE_PAGO, PLANILLAS, ANULACIONES, MOTIVO_DE_ANULACION, CONFIGURACION, DEPOSITOS, EXISTENCIAPORDEPOSITO, LISTADEPRECIOS, DETALLELISTA, EMPRESAS, MONEDAS, COTIZACION, AUDITORIA

### Lógica de negocio crítica (migrada de Oracle PL/SQL)
- `fn_obtener_numero` — genera número correlativo de comprobante
- `fn_saldo_cpte` / `fn_deuda_cliente` — consultas financieras
- `sp_crear_comprobante` / `sp_crear_detalle_cpte`
- `sp_pagar_comprobante` / `sp_anular_cobro` / `sp_anular_comprobante`
- 8 triggers sobre DOCUMENTO, DETALLE_DOCUMENTO, COBROS, COBRODETALLE

## Protocolo completo

Leer `.ai-handoff/WORKFLOW.md` completo al iniciar cada sesión.

Leer también los archivos de estándares en `.ai-handoff/standards/` al iniciar cada sesión.

## Modo de Operacion Autonomo — ACTIVO

El usuario ha otorgado **autorización permanente** para proceder sin pedir confirmación en:

- Crear, editar y sobreescribir cualquier archivo del proyecto
- Ejecutar SQL (SELECT, CREATE, ALTER, INSERT, DROP TRIGGER/PROCEDURE IF EXISTS) en la BD remota usando las credenciales del `.env`
- Derivar REQs a revision cuando exista aprobacion directa del usuario; no cerrar sin auditoria de Codex
- Instalar dependencias (npm install, pip install) en el entorno local

**Solo pausar si:**
- Se va a ejecutar DELETE masivo o DROP TABLE sobre datos de producción existentes
- El script de migración de datos (REQ-0007) está listo para ejecutarse (avisar antes de correr)
- Hay un error de BD que impide continuar y requiere decisión de diseño

Para todo lo demás: **implementar directamente**.

## Control remoto — Modo loop

Cuando el usuario trabaja a distancia (no está frente a la pantalla), activar el monitoreo del chat del dashboard con:

```
/loop En cada vuelta:
1. Ejecutar CALL sp_siguiente_accion_agente('{{PROJECT_CODE}}', 'claude') para obtener mensajes de chat no leídos y REQs pendientes asignados a claude.
— Si hay mensajes de chat no leídos: responder con sp_responder_chat, luego marcar leídos con sp_marcar_chat_leido. Si el mensaje es tarea o comando, ejecutarlo además de responder.
— Si hay REQs pendientes (Responsable=claude, Estado distinto de CERRADO/CANCELADO, EstadoOperativo distinto de BLOQUEADO_POR_PRIORIDAD): actuar según lo acordado.
— Si no hay nada pendiente: no hacer nada.
2. Esperar con un único sleep 90 y volver a empezar.
```

**Habilitar:** copiar y pegar el comando `/loop` de arriba en la sesión activa.
**Deshabilitar:** escribir cualquier mensaje normal — el loop se apaga automáticamente.

## Regla Operativa Compacta — BD-First

Cuando `PROJECT_DB_*` exista, la única fuente operativa es la BD. Los agentes deben leer trabajo con `sp_siguiente_accion_agente`, registrar observaciones con `sp_registrar_observacion`, cambiar estado/responsable solo con `sp_derivar_req`, y tratar `to_codex.md`, `to_claude.md` y `registry.jsonl` como mirrors de compatibilidad. No usar `codex:audit-mark` ni flujo de buzón salvo modo offline explícito.

Para lecturas operativas repetidas como buzon, estado del combo, REQs pendientes, dependencias y condiciones de corte de loop, usar acceso directo a la base por MySQL CLI, ODBC o el cliente DB local configurado siempre que `PROJECT_DB_HOST` sea alcanzable desde la estacion de trabajo. No subir scripts temporales al servidor para consultas repetitivas. Preferir objetos reutilizables de base de datos, como vistas y procedimientos almacenados; el template debe proveer o documentar un procedimiento de estado/combo que retorne conteos pendientes, trabajo por agente, bloqueos, items accionables y una bandera clara de corte de loop. Reservar SSH para despliegues, operaciones sobre archivos del hosting o fallback documentado cuando no haya acceso directo a la DB.

## Recordatorios operativos

- Al cerrar un REQ via `sp_derivar_req`, actualizar opcionalmente `registry.jsonl` y `events.jsonl` como mirror de compatibilidad.
- Si se actualizan mirrors `to_claude.md` / `to_codex.md`, dejarlos en `ESPERA` y consistentes con la BD. Nunca como señal operativa principal.
- El campo `REQ:` en los buzones puede contener múltiples REQs separados por coma cuando aplica.
- `codex-review.md` lo pre-crea el implementador vacío; Codex lo rellena durante la auditoría.
- Los scripts SQL van en `gestion_web/database/`, el backend en `gestion_web/backend/`, el frontend en `gestion_web/frontend/`.
- **PowerShell**: reglas estrictas para evitar confirmaciones de seguridad:
  - No usar subexpresiones `$()` en comandos ni strings.
  - No interpolar variables en strings con comillas dobles (evitar `"texto $variable"`).
  - Para mostrar texto + variable usar comas: `Write-Output "Exit:", $LASTEXITCODE`
  - O concatenar con `+`: `Write-Output ("Exit: " + $LASTEXITCODE)`
  - Para lógica compleja, escribir un script `.ps1` y ejecutarlo.
  - Usar variables intermedias: `$path = "..."; Get-Content $path`

## Comandos de PowerShell

Evita que Claude Code tenga que pedir confirmacion de seguridad siguiendo estas reglas:

- No uses subexpresiones `$()` dentro de comandos ni strings.
- No interpoles variables dentro de strings con comillas dobles, por ejemplo `"Exit: $code"`.
  PowerShell los trata como expandable strings y pueden disparar confirmacion.
- Para mostrar texto + variable, usa comas en lugar de interpolacion:

```powershell
Write-Output "Exit:", $LASTEXITCODE
```

- O concatena con el operador `+`:

```powershell
Write-Output ("Exit: " + $LASTEXITCODE)
```

- Para logica de varias lineas, escribe un script `.ps1` y ejecutalo.

Ejemplos, en lugar de:

```powershell
Write-Output "Exit: $LASTEXITCODE"
Write-Host "$r : $($content.Trim().Length) chars"
```

Hazlo asi:

```powershell
Write-Output "Exit:", $LASTEXITCODE
$len = $content.Trim().Length
Write-Host ($r + " : " + $len + " chars")
```

## Base de Coordinacion Multiagente

La base de datos es la fuente durable para estados, chat, eventos y observaciones. Los archivos `.ai-handoff/` se mantienen como espejo de compatibilidad para herramientas locales y artefactos de REQ (req.md, codex-review.md, etc.).

Variables requeridas: `PROJECT_DB_HOST`, `PROJECT_DB_PORT`, `PROJECT_DB_USER`, `PROJECT_DB_PASS`, `PROJECT_DB_NAME`, `PROJECT_CODE`.

Procedimientos disponibles para Claude:

| Procedimiento | Uso |
|---|---|
| `sp_siguiente_accion_agente(proyecto, agente)` | Leer chat no leído y REQs pendientes asignados al agente |
| `sp_derivar_req(proyecto, req, estado, responsable, actor, resumen)` | Cambiar estado + responsable de un REQ de forma atómica |
| `sp_crear_req(proyecto, codigo, titulo, desc, estado, responsable, actor)` | Crear o upsert un REQ |
| `sp_modificar_req(proyecto, req, titulo, desc, rama, responsable, nota, actor)` | Modificar campos de un REQ |
| `sp_responder_chat(proyecto, canal, rol, autor, contenido)` | Enviar mensaje al chat del canal indicado |
| `sp_marcar_chat_leido(proyecto, agente)` | Marcar mensajes de chat como leídos |
| `sp_registrar_observacion(proyecto, req, auditor, ronda, categoria, subcategoria, resumen, archivo, severidad)` | Registrar observación de auditoría |

Si la base y los archivos no coinciden, usar la base como fuente principal y reconciliar los archivos antes de avanzar. Si la base no está disponible, registrar el fallo y continuar con flujo de archivos.
