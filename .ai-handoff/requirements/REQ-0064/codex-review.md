# REQ-0064 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-12
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos Bloqueantes

- `SeguridadService.cambiarPassword` valida el historial, cambia el hash y luego inserta en `password_historial` el hash nuevo. No guarda el hash anterior antes del cambio. Como consecuencia, despues del primer cambio un usuario puede volver a una contrasena antigua que nunca quedo en historial, incumpliendo la anti-reutilizacion de las ultimas N.

## Solucion Esperada

- Antes de reemplazar `password_hash`, guardar el hash anterior en `password_historial`.
- Luego guardar el nuevo hash si se quiere conservar trazabilidad completa, pero la validacion de reutilizacion debe cubrir hashes anteriores reales.
- Considerar migracion/seed inicial de historial para usuarios existentes cuando cambien por primera vez.

## Pruebas Revisadas

- Revision estatica de `SeguridadService`, `SeguridadPoliticaService`, `seguridad.xhtml` y `V45__politicas_seguridad.sql`.
- Build Maven previo: `mvn -q clean package` EXIT 0.
