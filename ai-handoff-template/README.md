# AI Handoff Template

Plantilla para trabajar con requerimientos numerados, un agente implementador (Claude) y un agente auditor (Codex), con trazabilidad completa de cada cambio.

## Instalación rápida

### 1. Copiar la plantilla al proyecto destino

```text
.ai-handoff/
tools/
CLAUDE.md
.gitignore    ← merge con el .gitignore existente
```

### 2. Ajustar PROJECT_STATE.md

Editar `.ai-handoff/PROJECT_STATE.md` con:
- Ruta absoluta del proyecto
- Nombres de los agentes
- Notas operativas propias del proyecto

### 3. Ajustar CLAUDE.md

Revisar `.ai-handoff/../CLAUDE.md` y agregar cualquier instrucción específica del proyecto (paths, reglas de negocio, restricciones).

### 4. Agregar scripts al `package.json`

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

> `buzon:once` y `codex:audit-mark` son scripts legacy del flujo de archivos. En modo BD-first (cuando `PROJECT_DB_*` está configurado), los agentes leen trabajo con `sp_siguiente_accion_agente`.

### 5. En Codex Desktop, activar autolectura como `heartbeat`

```text
kind: heartbeat
destination: thread
rrule: FREQ=MINUTELY;INTERVAL=1
```

No usar cron para este caso.

### 6. Verificar

```bash
npm run handoff:check
# → HANDOFF CHECK: OK
```

Verificar también que la BD de coordinación está operativa:

```bash
npm run db:verify
```

---

## Crear un nuevo REQ

```bash
node tools/new-req.js Descripcion corta del requerimiento
# → REQ-0001 creado con todos los archivos de documentación
```

El script calcula el número siguiente automáticamente, crea la carpeta y registra la entrada en `registry.jsonl`.

---

## Regla central

Cuando `PROJECT_DB_*` está configurado, la BD es la única fuente operativa. Los agentes leen trabajo con `sp_siguiente_accion_agente`, registran observaciones con `sp_registrar_observacion` y transicionan estados con `sp_derivar_req`. Los archivos `to_codex.md`, `to_claude.md` y `registry.jsonl` son mirrors de compatibilidad.

| Quién actúa | Cómo |
|---|---|
| Implementador (Claude) envía a Codex | `npm run handoff:ready -- REQ-XXXX` → llama `sp_derivar_req` internamente |
| Auditor (Codex) transiciona | `sp_derivar_req('...', 'REQ-XXXX', 'CERRADO'/'REQUIERE_CAMBIOS', ...)` |

Nada se cierra sin trazabilidad en `.ai-handoff/requirements/`.

Antes de enviar un REQ o batch a Codex, Claude debe ejecutar:

```bash
npm run handoff:check
```

El precheck bloquea `test-plan.md` vacio, criterios pendientes en `req.md`, falta de manifiesto, lotes de mas de 5 REQs sin `BATCH_GRANDE_APROBADO_POR_USUARIO`, y saltos sobre REQs menores pendientes.

Para reenviar un REQ puntual ya corregido:

```bash
npm run handoff:ready -- REQ-XXXX
```

`handoff:ready` es la compuerta normal de envio: exige `preaudit-checklist.md` completo, consulta `AUDITORIA_OBSERVACION` y bloquea si queda cualquier observacion `pendiente`. Si el proyecto no usa base de datos de observaciones, definir conscientemente `HANDOFF_OBSERVATIONS_OPTIONAL=1`; por defecto la falta de tabla/configuracion bloquea el envio para evitar revisiones ciegas.

La prioridad transversal es cerrar primero el menor REQ pendiente. Si se aprende una regla repetible durante una correccion o auditoria, convertirla en estandar y aplicarla a los REQs pendientes antes de reenviarlos.

## Base de coordinacion multi-proyecto

La plantilla incluye `db/` para centralizar estado, buzones, chat, revisiones, observaciones, checklist y artefactos en una base multi-proyecto. Configurar estas variables:

```text
PROJECT_DB_HOST=
PROJECT_DB_PORT=3306
PROJECT_DB_USER=
PROJECT_DB_PASS=
PROJECT_DB_NAME=
PROJECT_CODE=
```

Aplicar y verificar:

```bash
npm run db:apply -- db/001_project_coordination_schema.sql
npm run db:apply -- db/002_agent_communication_procedures.sql
npm run db:migrate-handoff
npm run db:migrate-observations
npm run db:verify
```

Cuando `PROJECT_DB_*` esta configurado, la base es la fuente durable y `.ai-handoff/` queda como espejo de compatibilidad.

---

## Documentación

| Archivo | Contenido |
|---|---|
| `CLAUDE.md` | Instrucciones de sesión para Claude — qué hacer al arrancar |
| `.ai-handoff/WORKFLOW.md` | Contrato operativo completo: roles, estados, flujo, excepciones |
| `.ai-handoff/standards/` | Estandares reutilizables, independientes del lenguaje cuando corresponde |
| `.ai-handoff/PROJECT_STATE.md` | Contexto resumido del proyecto para retomar sesiones |
| `docs/SETUP.md` | Guía de instalación detallada |
| `tools/new-req.js` | Crea carpeta de REQ con todos los templates |
| `tools/buzon-watch.js` | Monitor de buzones — legado, solo modo offline sin BD |
| `tools/codex-audit-state.js` | Deduplicación por archivo — legado, solo modo offline sin BD |
| `tools/handoff-check.js` | Valida registry, eventos y prioridad de REQs antes de avanzar batches |
| `tools/handoff-ready.js` | Valida y reenvia de forma segura uno o varios REQs puntuales |
