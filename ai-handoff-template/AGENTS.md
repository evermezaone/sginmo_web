# Agent Entry Point - Gestion ONE Web

This repository is worked by multiple AI agents. Before doing any task, identify which agent you are and load the matching instruction file.

## Routing

- If you are Codex, read `CODEX.md` first.
- If you are Claude Code, read `CLAUDE.md` first.
- If you are another agent, read both `CODEX.md` and `CLAUDE.md`, then follow the role assigned by the user.

## Shared Rule

The project coordination DB is the durable source of truth when `PROJECT_DB_*` is configured. The handoff files under `.ai-handoff/` are the compatibility mirror for active work and local tooling. Agent-specific files define responsibilities; they do not replace the handoff protocol.

Use stored procedures for cross-agent communication when DB access is available:

| Procedure | Purpose |
|---|---|
| `sp_siguiente_accion_agente(project, agent)` | Read unread chat + pending REQs for this agent |
| `sp_derivar_req(project, req, estado, responsable, actor, resumen)` | Atomically change REQ state and owner |
| `sp_crear_req` / `sp_modificar_req` | Create or update a REQ |
| `sp_responder_chat(project, canal, rol, autor, contenido)` | Send a chat message |
| `sp_marcar_chat_leido(project, agente)` | Mark chat messages as read |
| `sp_registrar_observacion(project, req, auditor, ronda, categoria, subcategoria, resumen, archivo, severidad)` | Persist an audit finding |

Always prioritize the lowest-numbered pending REQ. If a reusable lesson appears during implementation or audit, update the standards and apply it to the remaining pending REQs before moving on.
