# Base de coordinacion multi-proyecto

Scripts principales:

```text
db/001_project_coordination_schema.sql
db/002_agent_communication_procedures.sql
```

Base objetivo:

```text
u237417599_project
```

Aplicacion sugerida desde Node:

```bash
npm run db:apply -- db/001_project_coordination_schema.sql
npm run db:apply -- db/002_agent_communication_procedures.sql
npm run db:migrate-handoff
npm run db:migrate-observations
npm run db:verify
```

El script crea una estructura multi-proyecto para centralizar:

- proyectos y agentes;
- requerimientos y estados;
- eventos append-only;
- mensajes de chat e historial;
- locks de agentes;
- revisiones de auditoria;
- observaciones de auditoria;
- checklist por etapa;
- artefactos/evidencias;
- vista `vw_req_estado_operativo`;
- procedimientos de estado, revision, observaciones, chat y locks.

Regla central:

`REQ.Estado` conserva la fuente de verdad y `vw_req_estado_operativo.EstadoOperativo` calcula lo que debe ver el dashboard, por ejemplo `BLOQUEADO_POR_PRIORIDAD` o `EN_COLA_CODEX`.

Procedimientos clave:

- `sp_siguiente_accion_agente`
- `sp_derivar_req`
- `sp_responder_chat`
- `sp_marcar_chat_leido`
- `sp_registrar_revision`
- `sp_registrar_observacion`
- `sp_crear_req`
- `sp_modificar_req`
