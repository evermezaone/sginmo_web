# REQ-0000 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-04
**Rama:** main

## Manifiesto Minimo Para Codex

- REQ: REQ-0000
- Tipo de cambio: configuracion
- Riesgo: medio
- Archivos clave:
  - `.gitignore`: exclusiones de secretos/builds/herramientas
  - `.env` (NO versionado): credenciales coordinacion + datos VPS (VPS_HOST/PORT/USER; VPS_PASS vacio — se usa clave)
  - `~/.ssh/sginmo_vps(.pub)` + alias `sginmo-vps` en `~/.ssh/config` (fuera del repo)
- Comandos probados:
  - `git push -u origin main`: OK — remoto `https://github.com/evermezaone/sginmo_web.git`, rama main publicada (EXIT:0)
  - `git ls-files | Select-String '\.env|tmp_my|mysql2026'`: sin coincidencias (EXIT:0 — sin secretos versionados)
  - `ssh sginmo-vps "echo CONEXION_OK"`: CONEXION_OK con autenticacion por clave (EXIT:0)
- Cambios de datos: no
- Cambios de entorno: si — clave SSH dedicada y alias en la estacion del usuario
- Impacto LLM/tokens: no
- Decision esperada: aprobar
- Notas para auditor: la contrasena de la VPS NUNCA se almaceno (opcion A: el usuario autorizo la clave publica el mismo). `VPS_PASS` existe en `.env` como campo opcional y esta vacio.

## Resumen Funcional

Repo git publicado en GitHub y acceso SSH por clave a la VPS destino, con relevamiento completo del servidor.

## Procedimiento de conexion (documentado)

```
ssh sginmo-vps          # alias -> edm@77.237.235.69:44044, clave ~/.ssh/sginmo_vps
```

## Relevamiento de la VPS (2026-07-04)

| Item | Valor |
|---|---|
| Host | vmi3296290 — 77.237.235.69:44044, usuario edm |
| SO | Ubuntu 24.04.4 LTS (kernel 6.8) |
| Recursos | 12 vCPU · 47 GB RAM · disco 242 GB (16 GB usados, 226 GB libres) |
| Java | NO instalado (se instala JDK 21 en REQ-0032) |
| PostgreSQL | NO instalado (se instala 16 en REQ-0032) |
| Docker | NO instalado |
| nginx | instalado, escuchando en 80/443 (reverse proxy futuro) |
| MySQL | local en 127.0.0.1:3306 (+33060) — servicio existente, no tocar |
| Otros puertos | 3000 (servicio existente), 631 (cups), 44044 (sshd) |
| 8080 | LIBRE (candidato para WildFly detras de nginx) |
| sudo | requiere contrasena → la instalacion de paquetes (REQ-0032) se coordina con el usuario |

## Cambios De Datos

Sin cambios.

## Variables De Entorno

`VPS_HOST`, `VPS_PORT`, `VPS_USER` en `.env` (no versionado). `VPS_PASS` vacio a proposito.

## Pruebas Ejecutadas

Ver test-plan.md.

## Riesgos Conocidos

- En la VPS conviven servicios existentes (MySQL local, nginx, puerto 3000): el aprovisionamiento de REQ-0032 no debe tocarlos.
