# Agent Entry Point - SGInmo Web (SGI)

This repository is worked by multiple AI agents. Before doing any task, identify which agent you are and load the matching instruction file.

## Routing

- If you are Codex, read `CODEX.md` first.
- If you are Claude Code, read `CLAUDE.md` first.
- If you are another agent, read both `CODEX.md` and `CLAUDE.md`, then follow the role assigned by the user.

## Shared Rule

The project coordination DB (`u237417599_project`, `PROJECT_CODE=SGI`) is the durable source of truth when `PROJECT_DB_*` is configured (see `.env`). The handoff files under `.ai-handoff/` are the compatibility mirror.

| Procedure | Purpose |
|---|---|
| `sp_siguiente_accion_agente('SGI', agente)` | Read unread chat and pending REQs assigned to the agent |
| `sp_derivar_req('SGI', req, estado, responsable, actor, resumen)` | Atomically change REQ state and responsable |
| `sp_crear_req` / `sp_modificar_req` | Create / update a REQ |
| `sp_responder_chat` / `sp_marcar_chat_leido` | Inter-agent chat |
| `sp_registrar_observacion` | Persist an audit observation |

Always prioritize the lowest-numbered pending REQ. If a reusable lesson appears during implementation or audit, update `.ai-handoff/standards/` and apply it to the remaining pending REQs before moving on.

Business-rule source of truth: `migracion\docs-migracion\` (docs 00-07). Legacy code: `Pysistemas\Inmobiliaria\`. New application: `migracion\Desarrollo\sginmo-web\`.
