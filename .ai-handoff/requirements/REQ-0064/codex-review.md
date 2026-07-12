# REQ-0064 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-12
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Hallazgos Bloqueantes

- Sin hallazgos bloqueantes en la re-auditoria.

## Evidencia de Re-auditoria

- `SeguridadService.cambiarPassword` valida la clave actual, rechaza que la nueva sea igual a la actual, consulta las ultimas N claves retiradas y compara con BCrypt.
- Antes de reemplazar el hash activo, captura `hashAnterior = u.getPasswordHash()`.
- Luego actualiza `passwordHash` con bcrypt nuevo y persiste `hashAnterior` en `password_historial`, por lo que la anti-reutilizacion cubre la clave actual y las claves retiradas reales.

## Pruebas Revisadas

- Revision estatica de `SeguridadService`, `SeguridadPoliticaService`, `seguridad.xhtml` y `V45__politicas_seguridad.sql`.
- Build Maven previo: `mvn -q clean package` EXIT 0.
