# REQ-0000 - Plan De Pruebas

**Fecha:** 2026-07-04

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `git init` + commit inicial + commits de trabajo | historial limpio en `main` | **OK** — bec5aa2, 30bab12, bfa01e7, 1386485 |
| T02 | `git ls-files` filtrado por `.env`, `tmp_my`, `mysql2026` | sin coincidencias (sin secretos) | **OK** — 0 coincidencias |
| T03 | `git push -u origin main` a GitHub | rama publicada | **OK** — `main -> main` en evermezaone/sginmo_web |
| T04 | Puerto VPS alcanzable | TcpTestSucceeded true | **OK** — 77.237.235.69:44044 |
| T05 | SSH por clave (BatchMode, sin password) | CONEXION_OK + whoami=edm | **OK** — tras autorizacion de clave por el usuario (opcion A) |
| T06 | Relevamiento del servidor | SO/recursos/servicios documentados | **OK** — tabla en claude-implementation.md |
| T07 | (Obs 201) `npm run handoff:check` tras corregir la consulta de AUDITORIA_OBSERVACION en handoff-check.js/handoff-ready.js | `HANDOFF CHECK: OK` EXIT:0 | **OK** — 2026-07-04: "HANDOFF CHECK: OK / LISTO_PARA_REVISION: REQ-0000, REQ-0001, REQ-0002", ExitCheck 0 |
| T08 | (Obs 201) Revision transversal de consumidores de AUDITORIA_OBSERVACION | todos con patron IdReq→REQ→PROYECTO + Estado=abierta | **OK** — grep en tools/: handoff-check.js, handoff-ready.js y handoff.py alineados; sin otros usos |

## Pruebas Manuales

| ID | Escenario | Pasos | Resultado esperado | Resultado real |
|---|---|---|---|---|
| M01 | Autorizacion de clave publica | usuario ejecuta el one-liner con su contrasena | queda en authorized_keys | **OK** — ejecutado por el usuario (opcion A) |

## Datos De Prueba

No aplica.
