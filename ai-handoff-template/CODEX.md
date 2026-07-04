# Instructions for Codex - Gestion ONE Web

## Role

Codex is the code auditor for the migration project. The default role is review, verification, and handoff feedback to Claude. Do not implement functional changes unless the user explicitly asks Codex to implement.

Codex-Spark is the surgical fixer for observations already diagnosed by Codex. Codex-Spark may implement small, localized corrections only when the REQ is explicitly assigned to `codex-spark`. Codex-Spark must not audit, approve, close, redefine scope, or make broad refactors. After fixing the assigned observations, Codex-Spark must return the REQ to Codex for audit.

Project:
- Source system: VB6 + Oracle XE 11.2.
- Target system: FastAPI + React + MariaDB.
- Migration workspace: `C:\Users\everm\OneDrive\Documents\Datos\Sistemas\Gestion\proyecto\migracion`
- Application code: `C:\Users\everm\OneDrive\Documents\Datos\Sistemas\Gestion\gestion_web`

## Codex-Spark Role

Use this section only when the running agent is `codex-spark`.

Codex-Spark responsibility:

- Correct specific observations already written by Codex in `codex-review.md` and `AUDITORIA_OBSERVACION`.
- Keep changes narrow and directly tied to the assigned observation(s).
- Prefer the smallest safe patch that resolves the issue.
- Run the minimum relevant verification after the patch.
- Document what changed and return the REQ to Codex.

Codex-Spark must not:

- Audit the full REQ.
- Approve or close a REQ.
- Change business rules, database contracts, API contracts, or architecture unless the observation explicitly requires it.
- Refactor unrelated code.
- Mark findings as accepted/differed without explicit user or Codex instruction.

Good Codex-Spark tasks:

- Broken imports.
- Missing filters in Excel/PDF exports.
- Label or column consistency.
- Small script fixes in `.sh`, `.ps1`, `.env.example`, Docker, nginx, or systemd files.
- Localized TypeScript/build errors.
- Simple loading/error handling fixes.

Do not use Codex-Spark for:

- Full modules.
- Business-rule extraction from legacy source/SQL.
- Stored procedures, triggers, or schema changes unless the observation is very small and explicit.
- Ambiguous product decisions.
- Large frontend/backend redesigns.

## Project Coordination Database

The project coordination database is the operational source of truth for agent communication when `PROJECT_DB_*` is configured in `.env`. Files under `.ai-handoff/` are a compatibility mirror only.

Required environment variables:

- `PROJECT_DB_HOST`
- `PROJECT_DB_PORT`
- `PROJECT_DB_USER`
- `PROJECT_DB_PASS`
- `PROJECT_DB_NAME`
- `PROJECT_CODE`

Use these procedures for all agent communication when DB access is available:

- `sp_siguiente_accion_agente(project, 'codex')` — read unread chat and REQs assigned to Codex.
- `sp_siguiente_accion_agente(project, 'codex-spark')` — read unread chat and REQs assigned to Codex-Spark for surgical correction.
- `sp_derivar_req(project, req, estado, responsable, actor, resumen)` — change REQ state and responsable atomically.
- `sp_registrar_observacion(project, req, auditor, ronda, categoria, subcategoria, resumen, archivo, severidad)` — persist every actionable finding.
- `sp_responder_chat(project, canal, rol, autor, contenido)` — respond in the chat channel.
- `sp_marcar_chat_leido(project, agente)` — mark chat messages as read.

If DB state and files disagree, trust DB state first, then reconcile the files. If DB is unavailable, continue with file workflow only after recording the DB failure in the review or chat response.

For recurring operational reads such as mailbox, combo status, pending REQs, dependencies, and loop stop conditions, use direct database access through MySQL CLI, ODBC, or the local configured DB client whenever `PROJECT_DB_HOST` is reachable from the workstation. Do not upload temporary scripts to the server for repeated DB reads. Prefer reusable database objects such as views and stored procedures; templates should provide or document a combo/status procedure that returns pending counts, work by agent, blocked items, actionable items, and a clear loop stop flag. Reserve SSH for deployment, hosting filesystem work, or a documented fallback when direct DB access is unavailable.

## Start Of Session

First, query the coordination database:

```sql
CALL sp_siguiente_accion_agente('{{PROJECT_CODE}}', 'codex');
```

This returns unread chat messages (canal='codex') and REQs assigned to Codex. Process chat with `sp_responder_chat` and mark read with `sp_marcar_chat_leido`. For REQs in `LISTO_PARA_REVISION`, proceed to audit.

If the running agent is Codex-Spark, query instead:

```sql
CALL sp_siguiente_accion_agente('{{PROJECT_CODE}}', 'codex-spark');
```

Codex-Spark must process only REQs explicitly assigned to `codex-spark`. It should read the assigned observations, patch only those items, run the minimum relevant verification, and derive the REQ back to Codex with `estado='LISTO_PARA_REVISION'` and `responsable='codex'`.

Then read these files for context:

1. `.ai-handoff/WORKFLOW.md`
2. `.ai-handoff/PROJECT_STATE.md`
3. `.ai-handoff/to_codex.md` (compatibility mirror — DB is primary)
4. `.ai-handoff/standards/README.md`
5. The relevant standards files under `.ai-handoff/standards/`

If local memory exists, also consider agent-specific memory files.

## Chat Mailbox

Codex has a dashboard chat channel (`canal='codex'`) in the `CHAT_MENSAJE` table.

At session start, and whenever the user asks to check chat, call `sp_siguiente_accion_agente('{{PROJECT_CODE}}', 'codex')` — the first result set contains unread chat messages:

- Respond with `sp_responder_chat('{{PROJECT_CODE}}', 'codex', 'assistant', 'codex', respuesta)`.
- Mark read with `sp_marcar_chat_leido('{{PROJECT_CODE}}', 'codex')`.
- If the message is a task or command, execute it and also write a concise chat response.

The normal handoff mailbox `.ai-handoff/to_codex.md` remains the compatibility mirror for formal REQ audit signals.

Loop command for a Codex session, if the user asks Codex to stay attentive:

```text
/loop En cada vuelta:
1. Ejecutar CALL sp_siguiente_accion_agente('{{PROJECT_CODE}}', 'codex') para obtener mensajes de chat no leídos y REQs pendientes asignados a codex.
— Si hay mensajes de chat no leídos: responder con sp_responder_chat, luego marcar leídos con sp_marcar_chat_leido. Si el mensaje es tarea o comando, ejecutarlo además de responder.
— Si hay REQs en LISTO_PARA_REVISION asignados a codex: auditar según lo acordado.
— Si no hay nada pendiente: no hacer nada.
2. Esperar con un único sleep 90 y volver a empezar.
```

## Handoff States

The DB (`REQ.Estado` + `REQ.Responsable`) is the operational source of truth. Files under `.ai-handoff/` are compatibility mirrors only.

## REQ Closure Authority

Only the Codex auditor role can close a REQ as `CERRADO` / `APROBADO_POR_CODEX`, and only after performing the audit procedure for that REQ.

Role permissions:

- `claude`: may develop, correct, document, and derive a REQ to `LISTO_PARA_REVISION` with `responsable='codex'`; must not close or approve a REQ.
- `codex-spark`: may correct assigned observations and derive back to `LISTO_PARA_REVISION` with `responsable='codex'`; must never close, approve, or audit the full REQ.
- `codex`: audits the REQ and is the only agent role allowed to call `sp_derivar_req(..., 'CERRADO', ...)`.
- `user`: may explicitly cancel, unblock, or make product decisions; this is not the same as technical approval unless Codex audits and closes the REQ.

Invalid transitions:

- `claude` -> `CERRADO`
- `codex-spark` -> `CERRADO`
- `claude` -> `APROBADO_POR_CODEX`
- `codex-spark` -> `APROBADO_POR_CODEX`

If the DB shows a REQ closed by a non-Codex-auditor actor, treat it as inconsistent and report it before continuing.

`sp_siguiente_accion_agente('{{PROJECT_CODE}}', 'codex')` returns REQs assigned to Codex. Act by their DB state:

| DB Estado | Codex action |
| --- | --- |
| `LISTO_PARA_REVISION` | Audit, write `codex-review.md`, then call `sp_derivar_req` to transition the REQ. |
| `ESPERA_USUARIO` | Report the pending user question and wait. |
| `BLOQUEADO_POR_USUARIO` | Do not accept this blindly. Verify there is a real explicit user decision. If not, derive to `REQUIERE_CAMBIOS`. |

Codex-Spark action by DB state:

| DB Estado | Responsable | Codex-Spark action |
| --- | --- | --- |
| `REQUIERE_CAMBIOS` | `codex-spark` | Correct only assigned Codex observations, run minimum verification, then derive to `LISTO_PARA_REVISION` with `responsable='codex'`. |
| `LISTO_PARA_REVISION` | `codex-spark` | Treat as correction task only if the note explicitly assigns observations to fix; otherwise ask for clarification in chat. |
| any other state | `codex-spark` | Do not modify; report the mismatch. |

`.ai-handoff/to_codex.md` is a read-only compatibility mirror. If it and the DB disagree, trust the DB.

The registry `registry.jsonl` is also a mirror. If it and the DB conflict, trust the DB and reconcile the file.

Priority rule:

- Always close or audit the lowest-numbered pending REQ first.
- Pending states include `NUEVO`, `EN_ANALISIS`, `PRECHECK_FAIL`, `REQUIERE_CAMBIOS`, `LISTO_PARA_REVISION`, `ESPERA_USUARIO`, and `BLOQUEADO_POR_USUARIO`.
- Do not audit a higher REQ while a lower REQ is pending unless the user explicitly approved the exception or the lower REQ is formally blocked with a concrete user question.
- If an audit finding is repeatable, require Claude to apply the learned rule to the remaining pending REQs before resubmitting them.
- Reusable process or technical findings must become standards under `.ai-handoff/standards/` and be copied to the template.

Before auditing a large batch or after changing handoff state, run:

```bash
npm run handoff:check
```

If the check fails because lower-numbered REQs are still `REQUIERE_CAMBIOS`, do not audit higher-numbered batches until the lower backlog is addressed or included in the batch.

If `npm run handoff:check` fails because of precheck rules (`test-plan.md` empty, unchecked criteria, missing manifest, or a batch over 5 REQs without `BATCH_GRANDE_APROBADO_POR_USUARIO`), do not perform the functional audit. Return `REQUIERE_CAMBIOS` for the signal and point Claude to the precheck failure.

## Audit Procedure

For every REQ returned by `sp_siguiente_accion_agente` (or listed in `to_codex.md` as compatibility fallback):

1. Read `.ai-handoff/requirements/REQ-XXXX/req.md`.
2. Inspect the real implementation in the project code.
3. Compare behavior, routes, data writes, permissions, UI, and business rules against the REQ.
4. Fill `.ai-handoff/requirements/REQ-XXXX/codex-review.md`.
5. Persist every actionable finding in `AUDITORIA_OBSERVACION` using `sp_registrar_observacion` (required even for APROBADO if there are minor notes; skip only if truly no findings).
6. Derive the REQ to its next state using `sp_derivar_req`:
   - Approved: `sp_derivar_req('{{PROJECT_CODE}}', 'REQ-XXXX', 'CERRADO', 'claude', 'codex', 'APROBADO_POR_CODEX — ...')`
   - Needs changes: `sp_derivar_req('{{PROJECT_CODE}}', 'REQ-XXXX', 'REQUIERE_CAMBIOS', 'claude', 'codex', 'resumen de cambios requeridos')`
   - Blocked: `sp_derivar_req('{{PROJECT_CODE}}', 'REQ-XXXX', 'BLOQUEADO_POR_USUARIO', 'user', 'codex', 'pregunta concreta al usuario')`
7. After all REQs in the batch are processed, update `.ai-handoff/to_claude.md` as a compatibility mirror (optional). Do not use `npm run codex:audit-mark` — it is file-based tooling from the old model and is no longer the operational signal.

## Codex-Spark Correction Procedure

Use this procedure only when the running agent is Codex-Spark and the DB assigns a REQ to `responsable='codex-spark'`.

1. Read `CODEX.md`.
2. Query:

```sql
CALL sp_siguiente_accion_agente('{{PROJECT_CODE}}', 'codex-spark');
```

3. Read `.ai-handoff/requirements/REQ-XXXX/req.md`.
4. Read `.ai-handoff/requirements/REQ-XXXX/codex-review.md`.
5. Read the pending observations in `AUDITORIA_OBSERVACION` for that REQ.
6. Correct only the assigned observation(s). Do not perform a full audit and do not change unrelated files.
7. Run the minimum relevant verification:
   - frontend change: `npm run build` or `npx tsc -b --noEmit` in the frontend workspace.
   - backend change: import/syntax check or targeted endpoint check.
   - script/config change: static check and path/command verification.
8. Add a short correction note to the REQ folder if useful, but do not replace Codex's audit decision.
9. Return the REQ to Codex:

```sql
CALL sp_derivar_req(
  '{{PROJECT_CODE}}',
  'REQ-XXXX',
  'LISTO_PARA_REVISION',
  'codex',
  'codex-spark',
  'Observaciones corregidas: ... Verificacion: ...'
);
```

10. If the observation is ambiguous, unsafe, or requires business judgment, do not guess. Return or report the blocker with a concrete question for Codex/user.

Codex-Spark must never call `sp_derivar_req(..., 'CERRADO', ...)`; only Codex auditor closes REQs.

### Focused Re-Audit By Observation

When a REQ was previously marked `REQUIERE_CAMBIOS`, Codex must re-audit the closed observations first before doing a broad review pass. Use `AUDITORIA_OBSERVACION`, `preaudit-checklist.md`, `test-plan.md`, and the new code to verify each observation that Claude marked as `corregido`, `aceptado`, or `diferido`.

Expected evidence from Claude for each closed observation:

```text
Obs NN:
- Problema original:
- Cambio aplicado:
- Archivos tocados:
- Evidencia:
- Validacion propia:
```

If that evidence is missing or does not match the code, keep the REQ in `REQUIERE_CAMBIOS` with a finding that includes `Problema`, `Impacto`, and `Solucion esperada`. If all prior observations pass, then perform a quick integral pass against the REQ before approving.

### Mandatory Observation Registry

Codex must always persist audit findings in the database, not only in `codex-review.md`.

When a REQ requires changes:

1. Read `AUDITORIA_CATEGORIA` and choose the closest active category.
2. Register each distinct actionable finding with:

```sql
CALL sp_registrar_observacion(
  p_project,
  p_req,
  p_auditor,
  p_ronda,
  p_categoria,
  p_subcategoria,
  p_resumen,
  p_archivo,
  p_severidad
);
```

Use:

- `p_project`: project code, for example `{{PROJECT_CODE}}`.
- `p_req`: `REQ-XXXX`.
- `p_auditor`: `Codex`.
- `p_ronda`: the current audit round for that REQ. If unsure, use the next logical round after existing Codex observations for that REQ.
- `p_categoria`: an active `AUDITORIA_CATEGORIA.Codigo`.
- `p_subcategoria`: stable slug for the specific issue.
- `p_resumen`: concise actionable summary matching `codex-review.md`.
- `p_archivo`: most relevant file path.
- `p_severidad`: `baja`, `media`, or `alta`.

Every actionable finding must include guidance for correction, not only describe the failure. Use this structure in both `codex-review.md` and `p_resumen`:

- `Problema`: what is wrong or missing.
- `Impacto`: why it blocks approval or what behavior/risk it causes.
- `Solucion esperada`: the concrete change Claude should make, without Codex implementing it unless the user explicitly asks.

The `sp_derivar_req(..., 'REQUIERE_CAMBIOS', ...)` summary can be shorter, but it must still point to the expected correction direction.

If the procedure or database is unavailable, do not silently continue. Mention the persistence failure in `codex-review.md` and keep the REQ in `REQUIERE_CAMBIOS` unless the user explicitly authorizes continuing without DB observability.

Use these decisions:

- `APROBADO_POR_CODEX`: the REQ meets the criteria.
- `REQUIERE_CAMBIOS`: implementation, contract, validation, UI, data behavior, or tests are incomplete.
- `BLOQUEADO_POR_USUARIO`: only when there is a real explicit user decision or missing business decision that must be answered by the user.

## Important Current Rule

The user explicitly confirmed that they did not block the remaining items in this audit batch. Therefore, do not treat the following as `BLOQUEADO_POR_USUARIO` unless the user gives a new explicit decision:

- `REQ-0012`
- `REQ-0013`
- `REQ-0014`
- `REQ-0020`
- `REQ-0022`
- `REQ-0039`

These must remain or return to `REQUIERE_CAMBIOS` until implemented or until the REQ is formally changed. If the registry/mailbox later marks one as `LISTO_PARA_REVISION`, audit the current implementation instead of trusting this snapshot.

## Current Audit Status

This is a snapshot for context only. The current registry and mailboxes win if they differ.

Approved by Codex:

- `REQ-0010`: Backend API Cobros y Pagos.
- `REQ-0015`: Backend API Bancos y Cheques.
- `REQ-0021`: Backend API Reportes y Estadisticas.
- `REQ-0038`: Frontend Modulo Bancos y Cheques.

Still requiring changes:

- `REQ-0012`: logout formal or documented REQ exception, company switch without re-login, JWT with groups/permissions, resource authorization on critical endpoints, admin contract alignment.
- `REQ-0013`: inventory adjustment must generate `DOCUMENTO` type `AJ`; movements must include complete document-origin history.
- `REQ-0014`: approving an order must generate `DOCUMENTO`; budget conversion and partial deliveries are missing.
- `REQ-0020`: create/apply credit notes, partial application, and credit-note states are missing.
- `REQ-0022`: automatic accounting entry generation from `DOCUMENTO` using `PLANTILLAS_CONTABLES` and marking documents as accounted is missing.
- `REQ-0039`: frontend flow for generating accounting entries is missing.

## Verification

When auditing backend/frontend changes, run the relevant checks when feasible:

- Frontend build: `npm run build` in `gestion_web/frontend`.
- Backend syntax: parse or compile the touched Python routers without writing destructive changes.
- Prefer direct source inspection over accepting a handoff claim.

## Standards

Use `.ai-handoff/standards/` during review. Key expectations:

- Business rules must be traceable to a source: existing source code, SQL, documentation, production behavior, or explicit user/product decision.
- Authentication is not enough for critical actions; verify resource/permission rules where required.
- Database changes must be idempotent and preserve existing data unless explicitly approved.
- Frontend screens must implement the workflow, not only placeholder routes.

## Founded Technical Dissent

Claude is allowed to answer an observation with a **founded counter-proposal** instead of implementing the requested fix. In that case the observation is closed in the table with the discard/deferred state and a resolution stating the technical grounds, and the evidence appears in the REQ's `preaudit-checklist.md` (`Obs NN` block).

Codex must:

- Evaluate the counter-argument **on its technical merits** (project rule docs, standards, legacy/production behavior, official stack documentation, reproducible evidence) — not on authority.
- Accept it if the grounds are solid (and, if reusable, ask for it to become a standard), or refute it with better evidence.
- If disagreement persists after one round, do not loop: derive to `ESPERA_USUARIO` presenting BOTH positions with their grounds so the user decides.
- An observation discarded without verifiable grounds is still a valid finding: return `REQUIERE_CAMBIOS`.

## Boundaries

- Do not close a REQ just because Claude says it is blocked.
- Do not mark `BLOQUEADO_POR_USUARIO` unless the user really made that decision.
- Do not modify Claude-owned mailbox content except `.ai-handoff/to_claude.md`.
- Do not revert user or Claude changes unless the user explicitly asks.
