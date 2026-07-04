# REQ-0000 - Analisis

**Estado:** EN_ANALISIS
**Fecha:** 2026-07-03
**Analista:** Implementador (Claude)

## Analisis Funcional

Infraestructura de trabajo: repositorio git del workspace (codigo + handoff + docs, sin secretos) con remoto GitHub, y acceso SSH operativo a la VPS destino con relevamiento del servidor.

## Analisis Tecnico

- Repo git en `migracion/` con `.gitignore` que excluye `.env`, `tmp_my.cnf`, historicos `mysql*.sql`, `herramientas/`, `target/`, `node_modules/`.
- Remoto: `https://github.com/evermezaone/sginmo_web.git` (decision del usuario).
- Acceso VPS: clave SSH dedicada ed25519 (`~/.ssh/sginmo_vps`) + alias `sginmo-vps` en `~/.ssh/config`. La clave la autorizo el usuario en el servidor (opcion A: nunca almacenamos la contrasena).

## Riesgos

| Riesgo | Probabilidad | Impacto | Mitigacion |
|---|---|---|---|
| Secretos versionados por error | baja | alto | .gitignore + verificacion `git ls-files` en cada envio |
| Perdida de la clave privada local | baja | medio | el usuario puede revocar en authorized_keys y regenerar |

**Semaforo Codex:** medio (infraestructura y seguridad de accesos)

## Preguntas Abiertas

- [x] Ninguna (VPS y remoto git provistos por el usuario)

## Impacto En Costos / LLM

- Puede resolverse sin IA: si

## Impacto En Datos

- Requiere migracion: no

## Recomendacion

**Desarrollar**
