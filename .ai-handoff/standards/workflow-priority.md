# Estandar De Prioridad Del Flujo

Este estandar es independiente del lenguaje, framework o stack tecnico del proyecto.

## Regla Principal

El flujo siempre debe intentar cerrar primero el REQ pendiente con numeracion menor.

Un REQ se considera pendiente si esta en cualquiera de estos estados del registry:

- `NUEVO`
- `EN_ANALISIS`
- `PRECHECK_FAIL`
- `REQUIERE_CAMBIOS`
- `LISTO_PARA_REVISION`
- `ESPERA_USUARIO`
- `BLOQUEADO_POR_USUARIO`

`CERRADO` y `CANCELADO` no bloquean el avance.

## Orden Operativo

1. Identificar el menor REQ pendiente en `registry.jsonl`.
2. Si esta del lado de Claude (`NUEVO`, `EN_ANALISIS`, `PRECHECK_FAIL`, `REQUIERE_CAMBIOS`), Claude debe corregir ese REQ antes de trabajar REQs mayores.
3. Si esta en `LISTO_PARA_REVISION`, Codex debe auditar ese REQ antes de auditar REQs mayores.
4. Si esta bloqueado por usuario, registrar la pregunta concreta y no avanzar a REQs mayores salvo aprobacion explicita del usuario.
5. Cuando se aprende una regla durante la correccion o auditoria del menor REQ, aplicar esa misma regla a los REQs mayores pendientes antes de reenviarlos.

## Reenvio Seguro

No editar a mano `registry.jsonl` y `to_codex.md` para reenviar un REQ puntual.

Usar:

```bash
npm run handoff:ready -- REQ-XXXX
```

El comando debe validar evidencia minima, limpiar marcas obsoletas y escribir un batch limpio solo con los REQs indicados.

## Estados Efectivos En Paneles

Las pantallas operativas no deben mostrar `LISTO_PARA_REVISION` como accionable si el flujo esta bloqueado por prioridad.

- Si existe un REQ menor en `REQUIERE_CAMBIOS`, `PRECHECK_FAIL`, `ESPERA_USUARIO` o `BLOQUEADO_POR_USUARIO`, cualquier REQ mayor que este en `LISTO_PARA_REVISION` debe mostrarse como `BLOQUEADO_POR_PRIORIDAD`.
- Si `to_codex.md` contiene una senal activa, solo los REQs listados en esa senal deben mostrarse como accionables para Codex; otros `LISTO_PARA_REVISION` pueden mostrarse como `EN_COLA_CODEX`.
- El registry conserva el estado fuente. El panel debe calcular un `estado_efectivo` derivado para evitar inconsistencias visuales.
- Ningun agente debe usar el panel como unica fuente de verdad para auditar o cerrar; siempre debe validar `registry.jsonl`, `to_codex.md`, `to_claude.md` y el menor REQ pendiente.

## Excepciones

Solo se puede saltar un REQ menor pendiente cuando:

- el usuario lo aprueba explicitamente;
- el REQ menor queda `CANCELADO`;
- el REQ menor queda `BLOQUEADO_POR_USUARIO` con una pregunta concreta registrada;
- el usuario aprueba un batch grande y el `MENSAJE:` incluye el token requerido por el workflow.

La excepcion debe quedar registrada en `events.jsonl` del REQ afectado y, si corresponde, en `registry.jsonl`.

## Aprendizaje Acumulado

Cada hallazgo repetible debe convertirse en una regla reutilizable:

- Si es regla de proceso, actualizar este archivo o `WORKFLOW.md`.
- Si es regla tecnica, actualizar el estandar del area (`backend-api.md`, `frontend-ui.md`, `database.md`, etc.).
- Si es regla del stack o shell, actualizar las instrucciones del agente correspondiente (`CLAUDE.md`, `CODEX.md`) y el template.
- Si aplica a varios REQs pendientes, Claude debe revisar esos REQs antes de reenviarlos.

Ejemplo: si Codex detecta que faltan comandos probados en `claude-implementation.md`, Claude debe verificar ese punto en todos los REQs pendientes antes de volver a ponerlos en `LISTO_PARA_REVISION`.
