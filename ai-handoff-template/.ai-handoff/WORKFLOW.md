# Workflow De Requerimientos Con Auditoria

Este documento define el contrato operativo para trabajar con requerimientos numerados, trazabilidad completa y auditoría entre agentes.

## Roles

### Usuario
- Describe requerimientos.
- Prioriza.
- Responde dudas.
- Prueba manualmente cuando corresponde.
- Aprueba o rechaza cierres.

### Implementador (Claude)
- Analiza antes de desarrollar.
- Documenta plan, implementación y pruebas.
- Responde observaciones del auditor.
- No cierra sin aprobación cuando el flujo lo requiere.

### Auditor (Codex)
- Revisa diffs, riesgos, pruebas, costos, datos, seguridad y consistencia funcional.
- Emite `APROBADO_POR_CODEX`, `REQUIERE_CAMBIOS` o `BLOQUEADO_POR_USUARIO`.
- Escribe la auditoría en `codex-review.md` (pre-creado vacío por el implementador).

---

## ¿Dónde Vive Qué?

Esta tabla es la fuente de referencia para saber dónde buscar o escribir cada tipo de información.

| Artefacto | Dónde vive | Notas |
|---|---|---|
| Estado del REQ (`NUEVO`, `CERRADO`, etc.) | **BD** — tabla `REQ` | Fuente primaria; `registry.jsonl` es mirror |
| Responsable actual del REQ | **BD** — tabla `REQ` | Fuente primaria |
| Observaciones de auditoría | **BD** — `AUDITORIA_OBSERVACION` | Fuente primaria; `codex-review.md` es mirror narrativo |
| Chat entre agentes | **BD** — `CHAT_MENSAJE` | Fuente primaria; no existe archivo equivalente |
| Eventos del REQ | **BD** — `REQ_EVENTO` | Fuente primaria; `events.jsonl` es mirror |
| `registry.jsonl` | Solo local | Mirror de `REQ.Estado`; válido cuando BD no está disponible |
| `to_claude.md` / `to_codex.md` | Solo local | Mirror de señales de transición; solo operativos en modo offline |
| `req.md` | Solo local | Documento de texto largo; no se almacena en BD |
| `claude-implementation.md` | Solo local | Ídem |
| `codex-review.md` | Solo local | Narrativa de auditoría; las observaciones accionables van en BD |
| `test-plan.md` | Solo local | Ídem |
| `preaudit-checklist.md` | Solo local | Ídem |
| `analysis.md`, `claude-plan.md` | Solo local | Ídem |

**Regla:** si BD y archivos locales discrepan, la BD manda. Reconciliar los archivos desde la BD antes de avanzar. Si la BD no está disponible, usar `registry.jsonl` y buzones como respaldo temporal y registrar el fallo.

---

## Estados Del REQ

La máquina de estados vive en la BD (`REQ.Estado`). `registry.jsonl` es el mirror local de compatibilidad.

| Estado | Significado | Responsable típico |
|--------|-------------|-------------------|
| `NUEVO` | Creado, sin análisis | claude |
| `EN_ANALISIS` | En análisis por el implementador | claude |
| `ESPERA_USUARIO` | Bloqueado esperando respuesta o decisión del usuario | user |
| `PRECHECK_FAIL` | Validacion local fallida; no debe enviarse a Codex | claude |
| `LISTO_PARA_REVISION` | Implementado, esperando auditoría de Codex | codex |
| `REQUIERE_CAMBIOS` | Codex rechazó la entrega; Claude debe corregir | claude |
| `BLOQUEADO_POR_USUARIO` | Requiere decisión de negocio del usuario | user |
| `CERRADO` | Completado y aprobado | — |
| `CANCELADO` | Descartado | — |

**Transición de estados:** siempre via `sp_derivar_req`. No editar `REQ.Estado` directamente ni editar `registry.jsonl` a mano para cambiar estados.

### Buzones — Solo Mirror De Compatibilidad

`to_claude.md` y `to_codex.md` son archivos de señal legibles por herramientas locales cuando la BD no está disponible. En modo normal (BD disponible), los agentes leen su estado desde `sp_siguiente_accion_agente` y escriben transiciones via `sp_derivar_req`. Los buzones se actualizan como mirror opcional al final del ciclo.

| Señal en buzón | Equivalente en BD |
|---|---|
| `to_codex.md` en `LISTO_PARA_REVISION` | `REQ.Estado=LISTO_PARA_REVISION`, `REQ.Responsable=codex` |
| `to_claude.md` en `REQUIERE_CAMBIOS` | `REQ.Estado=REQUIERE_CAMBIOS`, `REQ.Responsable=claude` |
| `to_claude.md` en `APROBADO_POR_CODEX` | `REQ.Estado=CERRADO`, `REQ.Responsable=claude` |
| `to_claude.md` en `ESPERA_USUARIO` | `REQ.Estado=ESPERA_USUARIO`, `REQ.Responsable=user` |

---

## Estructura

```text
AGENTS.md               ← punto de entrada comun; enruta a CODEX.md o CLAUDE.md
CODEX.md                ← instrucciones de sesion para Codex
CLAUDE.md               ← instrucciones de sesion para Claude
.ai-handoff/
  WORKFLOW.md            ← este archivo
  PROJECT_STATE.md       ← contexto del proyecto, actualizar por sesión
  to_claude.md           ← buzón Codex → Claude (espejo de compatibilidad; DB es primario)
  to_codex.md            ← buzón Claude → Codex (espejo de compatibilidad; DB es primario)
  requirements/
    registry.jsonl       ← espejo de compatibilidad; REQ.Estado en BD es primario
    _templates/
    REQ-0001/
      req.md
      analysis.md
      claude-plan.md
      claude-implementation.md
      codex-review.md    ← pre-creado vacío por Claude; Codex lo rellena
      test-plan.md
      preaudit-checklist.md ← checklist obligatorio de Claude antes de reenviar
      user-decision.md
      events.jsonl
```

---

## Flujo Operativo

1. Usuario envía uno o varios requerimientos.
2. Implementador crea carpeta(s) del REQ: `node tools/new-req.js <titulo>`.
3. Implementador completa `req.md` y `analysis.md`.
4. Si hay riesgo alto, impacto en datos o dudas de negocio, pide aprobación antes de desarrollar.
5. Implementador desarrolla y completa `claude-implementation.md`, `test-plan.md` y `events.jsonl`.
6. Implementador completa `preaudit-checklist.md` y ejecuta `npm run handoff:ready -- REQ-XXXX`. Ese comando transiciona el REQ en BD via `sp_derivar_req(..., 'LISTO_PARA_REVISION', 'codex', ...)` y actualiza `to_codex.md` como mirror. No editar a mano.
7. Auditor lee REQs asignados desde `sp_siguiente_accion_agente('{{PROJECT_CODE}}', 'codex')`, revisa código real y rellena `codex-review.md`.
8. Si requiere cambios: auditor llama `sp_derivar_req(..., 'REQUIERE_CAMBIOS', 'claude', 'codex', 'resumen')` y registra cada observación accionable con `sp_registrar_observacion`. Actualizar `to_claude.md` es opcional (mirror).
9. Si aprueba: auditor llama `sp_derivar_req(..., 'CERRADO', 'claude', 'codex', 'APROBADO_POR_CODEX')`. Actualizar `to_claude.md` es opcional (mirror).
10. Implementador consulta `sp_siguiente_accion_agente` al inicio de sesión para ver REQs en `REQUIERE_CAMBIOS`; corrige, cierra observaciones en `AUDITORIA_OBSERVACION` y reenvía con `npm run handoff:ready -- REQ-XXXX`.

### Regla De Prioridad: Menor REQ Pendiente

La regla mas sana del flujo es cerrar siempre primero el REQ pendiente con numeracion menor.

Estados que cuentan como pendientes:

- `NUEVO`
- `EN_ANALISIS`
- `PRECHECK_FAIL`
- `REQUIERE_CAMBIOS`
- `LISTO_PARA_REVISION`
- `ESPERA_USUARIO`
- `BLOQUEADO_POR_USUARIO`

Reglas:

1. Claude debe trabajar primero el menor REQ pendiente que este de su lado (`NUEVO`, `EN_ANALISIS`, `PRECHECK_FAIL`, `REQUIERE_CAMBIOS`).
2. Codex debe auditar primero el menor REQ pendiente que este en `LISTO_PARA_REVISION`.
3. No se debe saltar a un REQ mayor si existe uno menor pendiente, salvo aprobacion explicita del usuario o bloqueo documentado.
4. Si se aprende una regla corrigiendo o auditando el menor REQ, esa regla debe revisarse en los REQs mayores pendientes antes de reenviarlos.
5. Los lotes son validos solo si respetan esta prioridad o si el usuario aprueba explicitamente la excepcion.

### BD Como Fuente De Verdad

La base de coordinacion (`PROJECT_DB_*` en `.env`) manda siempre. Los archivos locales (`registry.jsonl`, `to_claude.md`, `to_codex.md`) son espejos de compatibilidad para herramientas locales y modo offline. Si DB y archivos discrepan, reconciliar archivos desde DB antes de avanzar.

Objetos DB obligatorios para comunicacion entre agentes:

- Tablas: `REQ`, `REQ_EVENTO`, `CHAT_MENSAJE`, `AUDITORIA_OBSERVACION`, `PROYECTO`.
- Vista: `vw_req_estado_operativo` — estado efectivo y bloqueo por prioridad.
- Procedimientos:
  - `sp_siguiente_accion_agente` — chat no leído + REQs pendientes del agente
  - `sp_derivar_req` — cambio atómico de estado + responsable
  - `sp_crear_req` / `sp_modificar_req` — alta y edición de REQs
  - `sp_responder_chat` / `sp_marcar_chat_leido` — mensajería inter-agente
  - `sp_registrar_observacion` — observaciones de auditoría

`registry.jsonl` y los buzones (`to_claude.md`, `to_codex.md`) son mirrors offline. En modo normal (BD disponible), los agentes leen y transicionan exclusivamente via DB. Los archivos se actualizan opcionalmente como mirror al final del ciclo.

Reglas obligatorias:

1. Si cualquier REQ menor esta pendiente (segun `sp_siguiente_accion_agente` o `registry.jsonl` en modo offline), el agente correspondiente debe atenderlo antes de avanzar a REQs de numeracion mayor.
2. Claude no debe reconciliar mirrors de forma que contradigan la BD; si la BD tiene REQs menores pendientes, no puede forzar un mirror de `ESPERA` o `LISTO_PARA_REVISION` que oculte ese backlog.
3. Si el mirror `to_codex.md` anuncia un batch mayor mientras existe un REQ menor pendiente en BD, ignorar el mirror y atender primero el backlog según la BD.
4. Antes de enviar cualquier batch a Codex, ejecutar:

```bash
npm run handoff:check
```

El comando debe pasar sin errores o el batch no debe enviarse a Codex.
Si falla el precheck, Claude debe dejar el REQ en `PRECHECK_FAIL` o continuar corrigiendo sin tocar `to_codex.md`.
5. Si `handoff:check` falla en un REQ que está en `LISTO_PARA_REVISION` en BD, derivarlo a `PRECHECK_FAIL` via `sp_derivar_req` (BD primero) y reconciliar `registry.jsonl` como mirror. El dashboard debe reflejar `PRECHECK_FAIL` y Codex no debe auditar ese lote.
6. Para reenviar uno o varios REQs ya corregidos, usar `npm run handoff:ready -- REQ-XXXX [REQ-YYYY]`. Ese comando valida la evidencia, exige `preaudit-checklist.md` completo, consulta `AUDITORIA_OBSERVACION`, cambia solo esos REQs a `LISTO_PARA_REVISION`, limpia marcas `precheck` obsoletas y escribe `to_codex.md` con un batch limpio.
7. Los dashboards y paneles deben mostrar un estado efectivo derivado: si un REQ menor bloquea el flujo, los `LISTO_PARA_REVISION` mayores deben verse como `BLOQUEADO_POR_PRIORIDAD`; si no estan en la senal activa de `to_codex.md`, deben verse como `EN_COLA_CODEX`.

---

### Auditoria Por Lote

Cuando existan varios REQs chicos, de bajo riesgo o relacionados, el implementador debe agruparlos en una sola entrega para Codex:

```text
REQ: REQ-0031, REQ-0032, REQ-0033
ESTADO: LISTO_PARA_REVISION
```

Reglas:

1. Solo agrupar REQs que puedan revisarse juntos sin ocultar riesgos.
2. No mezclar en el mismo lote cambios criticos de datos, precios, pedidos, WhatsApp o IA con cambios triviales.
3. Cada REQ mantiene su carpeta y trazabilidad propia.
4. `claude-implementation.md` de cada REQ debe incluir el manifiesto minimo.
5. Codex puede emitir una sola respuesta visible para el lote, pero debe dejar decision por REQ en los archivos `codex-review.md` cuando haya diferencias.
6. Un lote normal no debe superar 5 REQs. Para superar ese limite, el usuario debe aprobarlo explicitamente y `MENSAJE:` de `to_codex.md` debe incluir `BATCH_GRANDE_APROBADO_POR_USUARIO`.
7. No enviar lotes altos si hay REQs menores en `REQUIERE_CAMBIOS`.

### Preauditoria Obligatoria Antes De Enviar A Codex

Claude debe completar `.ai-handoff/requirements/REQ-XXXX/preaudit-checklist.md` antes de ejecutar `npm run handoff:ready -- REQ-XXXX`.

Si el REQ fue creado antes de existir ese archivo, Claude debe copiar la plantilla desde `.ai-handoff/requirements/_templates/preaudit-checklist.md`, reemplazar el ID y completar todos los items.

`handoff:ready` bloquea el reenvio si:

- Falta `preaudit-checklist.md`.
- Queda algun item `- [ ]` sin completar.
- Falta la firma `Responsable: Claude`.
- Existe cualquier observacion `pendiente` para ese REQ en `AUDITORIA_OBSERVACION`.

Las observaciones de auditoria no se cierran por texto libre. Antes de reenviar, Claude debe marcarlas en la tabla como `corregido`, `aceptado` o `diferido`, con nota suficiente para que Codex entienda la decision.

### Revision Transversal De Flujos Equivalentes

Una correccion no se considera lista si solo cubre el archivo donde Codex marco el error y deja el mismo patron en otro flujo.

Cuando una observacion toca una regla de negocio, validacion, estado, tabla, trigger, SP, endpoint, helper o componente compartido, Claude debe buscar y revisar flujos equivalentes antes de reenviar. La revision debe quedar documentada en `test-plan.md` o `preaudit-checklist.md`.

La revision transversal debe responder:

1. Que tabla, estado, endpoint, helper o componente compartido fue afectado.
2. Que busqueda se hizo para encontrar usos equivalentes.
3. Que archivos/rutas/flujos equivalentes se revisaron.
4. Si se corrigieron todos, se diferio alguno o no aplica.

Ejemplos de busquedas obligatorias:

- Si se cambia una regla para `COBRO`, revisar todos los lugares que insertan `COBRO` o `COBRO_DETALLE`.
- Si se cambia una regla de `PLANILLA.Estado`, revisar todos los endpoints que abren, cierran o asignan planillas.
- Si se corrige un permiso backend, revisar endpoints hermanos del mismo recurso.
- Si se corrige un helper de impresion, revisar todos los reportes/comprobantes que lo usan o deberian usarlo.

### Precheck Tecnico Obligatorio

Claude debe ejecutar `npm run handoff:check` y luego `npm run handoff:ready -- REQ-XXXX`; `handoff:ready` es el unico camino normal para derivar el REQ a `LISTO_PARA_REVISION` en BD (y actualizar `to_codex.md` como mirror).

El precheck bloquea:

- `test-plan.md` vacio o sin evidencia.
- Criterios de aceptacion sin marcar (`- [ ]`) en `req.md`.
- Falta de `Manifiesto Minimo Para Codex` en `claude-implementation.md`.
- Falta de comandos probados documentados.
- Falta de `preaudit-checklist.md` completo.
- Falta de evidencia de revision transversal cuando el cambio toca reglas o entidades compartidas.
- Observaciones pendientes en `AUDITORIA_OBSERVACION`.
- Batch con mas de 5 REQs sin override explicito del usuario.
- Batch que salta REQs menores pendientes (`PRECHECK_FAIL`, `REQUIERE_CAMBIOS`, `LISTO_PARA_REVISION`, bloqueados o en analisis).

### Observaciones Como Contrato

`AUDITORIA_OBSERVACION` es el contrato vivo entre Codex y Claude. Toda observacion accionable de Codex debe quedar registrada alli, y todo reenvio de Claude debe partir de esa tabla.

Cada observacion accionable debe orientar la correccion. Codex no debe limitarse a decir que algo falla: en `codex-review.md` y en `AUDITORIA_OBSERVACION.Resumen` debe incluir `Problema`, `Impacto` y `Solucion esperada`. La solucion esperada es una indicacion para Claude, no una autorizacion para que Codex implemente cambios salvo pedido explicito del usuario.

Cuando Claude reenvia un REQ que ya tuvo observaciones, debe responder cada observacion cerrada con evidencia puntual en `preaudit-checklist.md` o `test-plan.md`:

```text
Obs NN:
- Problema original:
- Cambio aplicado:
- Archivos tocados:
- Evidencia:
- Validacion propia:
```

Codex debe re-auditar primero esas observaciones cerradas contra el codigo real. Si pasan, hace una pasada integral breve del REQ antes de aprobar. Si falta evidencia o no coincide con el codigo, el REQ vuelve a `REQUIERE_CAMBIOS`.

Estados validos:

- `pendiente`: bloquea `handoff:ready`.
- `corregido`: Claude aplico la correccion y documento evidencia.
- `aceptado`: el hallazgo se acepta como riesgo residual o decision del usuario.
- `diferido`: queda fuera de alcance por decision documentada.

Reglas:

1. Codex debe registrar observaciones accionables al marcar `REQUIERE_CAMBIOS`.
2. Claude debe consultar las observaciones pendientes del menor REQ antes de corregir.
3. Claude debe cerrar o justificar cada observacion antes de reenviar.
4. Si una observacion representa una regla repetible, Claude debe aplicarla a REQs mayores pendientes antes de reenviarlos.

### Manifiesto Minimo Para Codex

El manifiesto no reemplaza el contexto del REQ. Es un indice obligatorio para que Codex audite con menos lecturas y abra solo lo necesario.

El implementador debe incluir en `claude-implementation.md`:

```text
## Manifiesto Minimo Para Codex

- REQ: REQ-XXXX
- Tipo de cambio: documental | UI | backend | BD | WhatsApp | IA | seguridad | configuracion
- Riesgo: bajo | medio | alto
- Archivos clave:
  - path/al/archivo.js: motivo
- Comandos probados:
  - npm test: resultado
- Cambios de datos: no | si, ver migracion
- Cambios de entorno: no | si, variables
- Impacto LLM/tokens: no | si, detalle
- Decision esperada: aprobar | revisar riesgo puntual | requiere criterio usuario
- Notas para auditor: puntos especificos a mirar
```

Codex debe empezar por este manifiesto y luego decidir si necesita abrir diffs, archivos del REQ o codigo. Para riesgo bajo y evidencia suficiente, puede auditar con lectura reducida. Para riesgo medio/alto, datos, pedidos, precios, WhatsApp, IA o seguridad, debe revisar evidencia completa.

---

## Formato De Los Buzones

Formato obligatorio (primeras líneas del archivo):

```text
ESTADO: ESPERA
REQ: -
TS: YYYY-MM-DDTHH:mm:ssZ
AGENTE: agente
MENSAJE: texto breve
```

El campo `REQ:` puede contener múltiples IDs separados por coma cuando la notificación abarca más de un REQ:

```text
REQ: REQ-0001, REQ-0002, REQ-0003
```

---

## Cuándo Se Puede Omitir La Auditoría De Codex

El paso por Codex es **obligatorio por defecto**. Se puede omitir únicamente en estos casos, registrando el motivo en `registry.jsonl` con el campo `"excepcion"`:

| Caso | Ejemplo |
|------|---------|
| Hotfix urgente con aprobación explícita del usuario | bug de producción bloqueante |
| Cambio de documentación pura (sin código) | actualizar README |
| REQ ya cubierto por otro REQ implementado y auditado | funcionalidad duplicada |
| Usuario prueba y aprueba directamente con el resultado ya visible | cambio de UI trivial ya verificado |

Ejemplo en `registry.jsonl`:
```json
{"req": "REQ-0005", "estado": "CERRADO", "excepcion": "Hotfix directo aprobado por usuario — bug bloqueante en producción"}
```

---

## Politica De Auditoria Codex

### Semaforo De Riesgo Para Codex

Cada REQ debe clasificarse en `analysis.md`:

| Riesgo | Auditoria Codex | Casos tipicos |
|---|---|---|
| bajo | Puede omitirse con aprobacion explicita del usuario | docs, textos, estilos menores, configuracion no critica |
| medio | Codex recomendado | UI funcional, endpoints simples, parsing, logs, reportes |
| alto | Codex obligatorio | datos, pedidos, precios, WhatsApp, IA/LLM, seguridad, autenticacion, migraciones |

Si se omite Codex por riesgo bajo, el implementador debe registrar la excepcion en `registry.jsonl`, `events.jsonl` y `codex-review.md`.

---

## Inicio De Sesion Del Auditor

Primero ejecutar:

```sql
CALL sp_siguiente_accion_agente('{{PROJECT_CODE}}', 'codex');
```

Eso devuelve chat no leído y REQs asignados a Codex. Procesar chat con `sp_responder_chat` y marcar leído con `sp_marcar_chat_leido`. Para REQs en `LISTO_PARA_REVISION`, proceder a auditar.

Luego leer los archivos de contexto:

- `.ai-handoff/PROJECT_STATE.md`
- `.ai-handoff/WORKFLOW.md`
- `CODEX.md` para reglas completas del rol auditor

`npm run buzon:once` queda disponible como herramienta de diagnóstico offline pero no es el paso operativo principal.

---

## Autolectura En Codex Desktop

Cuando el usuario pida que Codex quede atento, crear o verificar una automatización:

```text
kind: heartbeat
destination: thread
rrule: FREQ=MINUTELY;INTERVAL=1
```

Debe responder en el mismo hilo. No usar cron para este caso.

Respuesta sugerida:

```text
Auditado: aprobado REQ-0001.
Auditado: requiere cambios REQ-0001.
Sin tareas nuevas para Codex.
```

## Deduplicacion De Auditoria Codex

> **Nota:** En modo BD-first, la deduplicación es automática: `sp_siguiente_accion_agente` solo devuelve REQs en estado accionable asignados al agente. Si el REQ ya fue procesado (`CERRADO`, `REQUIERE_CAMBIOS` con `Responsable=claude`), no vuelve a aparecer. No se necesita marca local.

La deduplicación basada en archivos (`.codex-audit-state.json` + `npm run codex:audit-mark`) es el mecanismo **legacy para modo offline** (sin BD). Sigue disponible como respaldo:

- Clave de auditoría: `REQ|ESTADO|TS` de `to_codex.md`.
- Verificar `.ai-handoff/.codex-audit-state.json` antes de auditar en modo offline.
- `npm run codex:audit-mark` marca la señal del archivo como revisada.
- El archivo `.ai-handoff/.codex-audit-state.json` es runtime local; debe estar ignorado por git.

En modo normal (BD disponible), **no usar `codex:audit-mark`**.

Codex solo debe leer `.ai-handoff/WORKFLOW.md` y `.ai-handoff/PROJECT_STATE.md` al iniciar sesion o cuando el usuario indique que cambiaron. No debe releerlos en cada auditoria automatica.

---

## Seguridad

- No versionar secretos, logs, caches, credenciales ni archivos temporales.
- Ver `.gitignore` en la raíz del template para las exclusiones mínimas del sistema.

## Datos

Si un cambio requiere base de datos:

1. Documentar impacto en `analysis.md`.
2. Proponer migración en `claude-plan.md`.
3. Incluir SQL/migración en el mismo diff que el código dependiente.
4. No ejecutar cambios destructivos sin aprobación explícita.
