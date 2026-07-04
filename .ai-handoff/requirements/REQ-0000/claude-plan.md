# REQ-0000 - Plan De Implementacion

**Estado:** APROBADO_PARA_DESARROLLO
**Fecha:** 2026-07-03 (completado 2026-07-04 por observacion no bloqueante de Codex)

## Estrategia

1. Git: `git init` en `migracion/` + `.gitignore` con exclusiones de secretos/builds/herramientas + commit inicial; remoto GitHub y push (decision del usuario: `evermezaone/sginmo_web`).
2. Acceso VPS: generar clave ed25519 dedicada (`~/.ssh/sginmo_vps`) + alias `sginmo-vps` en `~/.ssh/config`; el usuario autoriza la clave publica el mismo (opcion A — la contrasena SSH no se almacena).
3. Verificacion: conexion `BatchMode` sin password + relevamiento del servidor (SO, recursos, servicios, puertos).
4. Tooling de compuerta: ante la incompatibilidad del `handoff-ready.js` del template con el esquema multiproyecto, adoptar `tools/handoff.py` (adaptado de FLUX) y corregir los tools JS al esquema real.

## Archivos A Modificar

| Archivo | Cambio |
|---|---|
| `.gitignore` | exclusiones del proyecto |
| `.env` (no versionado) | datos VPS + credenciales coordinacion |
| `tools/handoff.py` | compuerta BD-first (nuevo) |
| `tools/handoff-check.js` / `tools/handoff-ready.js` | consulta AUDITORIA_OBSERVACION corregida al esquema real (Obs 201) |
| `~/.ssh/config`, `~/.ssh/sginmo_vps*` | fuera del repo |

## Pruebas Previstas

- [x] push a GitHub OK
- [x] `git ls-files` sin secretos
- [x] SSH por clave OK + relevamiento
- [x] `npm run handoff:check` EXIT:0 tras corregir los tools (Obs 201)

## Riesgos

Ver analysis.md.

## Cambios De Datos

Sin cambios.
