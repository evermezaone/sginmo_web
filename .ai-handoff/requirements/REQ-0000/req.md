# REQ-0000 - Infraestructura: esquema git y acceso a VPS

**Numero:** REQ-0000
**Fecha de creacion:** 2026-07-03
**Estado inicial:** NUEVO
**Prioridad:** maxima (bloquea el resto por regla de menor REQ)

## Texto Original

"me gustaria darte acceso a la VPS donde vamos a instalar todo lo desarrollado y probar alli. entonces, no se si queres crear un REQ 000 donde seria montar todo el esquema git y accesos a la VPS"

## Objetivo Funcional

Dejar montada la infraestructura de trabajo del proyecto: repositorio git del workspace de migracion (codigo nuevo + handoff + docs, con exclusiones correctas) y acceso operativo a la VPS destino donde se instalara y probara lo desarrollado (WildFly 40 + PostgreSQL 16).

## Criterios De Aceptacion

- [x] Repositorio git inicializado en `migracion/` con `.gitignore` correcto (excluye `.env`, `tmp_my.cnf`, `herramientas/`, `target/`, `node_modules/`, runtime de handoff) y commit inicial.
- [x] Ningun secreto (credenciales BD, claves) queda versionado en git.
- [x] Datos de acceso a la VPS provistos por el usuario y verificados (conexion SSH exitosa desde la estacion).
- [x] Relevamiento de la VPS documentado: SO, recursos (CPU/RAM/disco), puertos disponibles, si ya tiene Java/PostgreSQL/otro servicio.
- [x] Procedimiento de conexion documentado en `claude-implementation.md` (sin credenciales en texto plano versionado; van en `.env`).
- [x] Decision registrada: remoto git (GitHub u otro) o solo repo local por ahora.

## Dependencias

- Depende de: datos de acceso a la VPS (usuario) — parcialmente ESPERA_USUARIO.
- Requerido por: REQ-0032 (provision servidor y deploy); el resto del desarrollo usa el repo git.
