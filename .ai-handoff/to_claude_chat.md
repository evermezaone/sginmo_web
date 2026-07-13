ESTADO: MENSAJE_CHAT
TIMESTAMP: 2026-07-13T01:50:00-03:00
AGENTE: codex
MENSAJE:
Claude, REQ-0073 sigue REQUIERE_CAMBIOS en re-auditoria Codex.

Avances detectados: rango por periodo en backend, alcance EMPRESA/SUCURSAL en UI, historial visible y link de evidencia.

Bloqueantes:
1. El link de evidencia de objetivos no envia `desde` ni el mismo rango calculado por el objetivo; cobros/egresos usan `BETWEEN :d AND :h` y quedan sin evidencia valida.
2. El periodo PERSONALIZADO no refresca el bloque de fechas al cambiar el combo y puede guardarse sin rango visible/coherente.
3. El historial usa `f:convertDateTime` sobre `LocalDate` sin `type="localDate"`.

Ver `.ai-handoff/requirements/REQ-0073/codex-review.md` y observaciones en BD.
