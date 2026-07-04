# Setup En Proyecto Nuevo

## 1. Copiar plantilla

Copiar al proyecto destino:

```text
.ai-handoff/
tools/
CLAUDE.md
```

Opcionalmente, mergear `.gitignore` con el del proyecto.

## 2. Ajustar PROJECT_STATE

Editar `.ai-handoff/PROJECT_STATE.md`:
- Reemplazar `RUTA_ABSOLUTA_DEL_PROYECTO` con la ruta real
- Ajustar nombres de agentes si difieren de Claude/Codex
- Agregar notas operativas del proyecto

## 3. Ajustar CLAUDE.md

`CLAUDE.md` le dice a Claude qué hacer al iniciar cada sesión. Revisarlo y agregar cualquier instrucción específica del proyecto:
- Paths de archivos clave
- Reglas de negocio a respetar
- Restricciones de arquitectura

> Este archivo es lo más importante del sistema para Claude Code.
> Sin él, Claude no sabe que debe consultar la BD / `sp_siguiente_accion_agente` al arrancar.

## 4. Agregar scripts al package.json

```json
{
  "scripts": {
    "handoff:check": "node tools/handoff-check.js",
    "handoff:ready": "node tools/handoff-ready.js",
    "new-req": "node tools/new-req.js",
    "db:apply": "node db/apply-schema.mjs",
    "db:migrate-handoff": "node db/migrate-handoff-to-db.mjs",
    "db:migrate-observations": "node db/migrate-review-observations-to-db.mjs",
    "db:verify": "node db/verify-project-data.mjs"
  }
}
```

Si el proyecto no usa Node, ejecutar los scripts directamente:
```bash
node tools/handoff-check.js
node tools/new-req.js <titulo>
```

> `buzon:once` y `codex:audit-mark` son herramientas legacy del flujo de archivos. En modo BD-first (cuando `PROJECT_DB_*` está configurado), los agentes leen trabajo con `sp_siguiente_accion_agente` y no necesitan esas herramientas.

## 5. Crear heartbeat en Codex Desktop

Prompt sugerido para el heartbeat:

```text
En cada vuelta:
1. Ejecutar CALL sp_siguiente_accion_agente('{{PROJECT_CODE}}', 'codex').
   — Si hay mensajes de chat no leídos: responder con sp_responder_chat, marcar leídos con sp_marcar_chat_leido.
   — Si hay REQs en LISTO_PARA_REVISION asignados a codex: auditar según WORKFLOW.md.
   — Si no hay nada pendiente: responder "Sin tareas nuevas para Codex."
No usar cron.
```

Configuración del heartbeat:

```text
kind: heartbeat
destination: thread
rrule: FREQ=MINUTELY;INTERVAL=1
```

## 6. Primer chequeo

```bash
node tools/handoff-check.js
# → HANDOFF CHECK: OK
```

`handoff-check` es obligatorio antes de derivar a Codex / ejecutar `handoff:ready`.
Bloquea lotes de mas de 5 REQs sin `BATCH_GRANDE_APROBADO_POR_USUARIO`, criterios pendientes en `req.md`, `test-plan.md` vacio, falta de manifiesto y saltos sobre REQs menores en `REQUIERE_CAMBIOS`.

## 7. Crear el primer REQ

```bash
node tools/new-req.js Mi primer requerimiento de prueba
# → ✓ Creado REQ-0001 — "Mi primer requerimiento de prueba"
```

Verificar que se creó:
- `.ai-handoff/requirements/REQ-0001/` con todos los archivos
- Entrada en `.ai-handoff/requirements/registry.jsonl`

---

## Archivos excluidos de versión control

El `.gitignore` del template cubre los archivos mínimos del sistema. Agregar al del proyecto:

```gitignore
.ai-handoff/.watcher-state.json
.ai-handoff/.codex-audit-state.json
```

Y según el proyecto:
```gitignore
.ai-handoff/.wa-qr.txt        # Si usa WhatsApp
.ai-handoff/.wa-status.json   # Si usa WhatsApp
logs/
uploads/
!uploads/**/.gitkeep
```
