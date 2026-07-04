# Estado Del Proyecto

Actualizado: YYYY-MM-DD HH:mm TZ

## Carpeta oficial

`RUTA_ABSOLUTA_DEL_PROYECTO`

## Protocolo vigente

- El proyecto usa `.ai-handoff/WORKFLOW.md` como contrato operativo.
- El implementador escribe `.ai-handoff/to_codex.md`.
- El auditor escribe `.ai-handoff/to_claude.md`.
- El auditor arranca o verifica tareas con `npm run buzon:once`.
- Si el buzon indica trabajo para auditoria, el auditor revisa y escribe el resultado en `codex-review.md`.
- Si aprueba, el implementador cierra segun `WORKFLOW.md`.
- Si pide cambios, el implementador corrige y reenvia a auditoria.

## Autolectura

En Codex Desktop, la lectura automatica debe ser:

```text
kind: heartbeat
destination: thread
rrule: FREQ=MINUTELY;INTERVAL=1
```

No usar cron si el usuario necesita ver la verificacion en el chat.

## Estado reciente

- Sin REQs recientes.

## Nota operativa

Este archivo resume contexto para retomar sesiones; no reemplaza `registry.jsonl`, `events.jsonl`, `codex-review.md` ni `WORKFLOW.md`.
