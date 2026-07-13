# REQ-0080 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-13
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Hallazgos

### Bloqueantes

- Ninguno.

### No Bloqueantes

- Se detectaron casts similares de `java.sql.Date` en otros servicios, pero no forman parte del alcance de este REQ. Si aparecen nuevos 500 con datos reales, conviene abrir un REQ tecnico separado para normalizar conversiones de fecha en servicios con native queries.

## Validacion

- `ComprobanteService#cobrosRecientes()` ya no castea `c.fecha` a `java.sql.Date`; usa `aLocalDate(Object)`.
- `ComprobanteService#reciboCobro()` aplica el mismo helper para la fecha del recibo.
- El helper acepta `java.time.LocalDate`, `java.sql.Date` y `java.sql.Timestamp`, y deja un fallback por `LocalDate.parse(o.toString())`, evitando dependencia directa del tipo devuelto por Hibernate/PostgreSQL para columnas `date`.

## Riesgos

Ninguno identificado para el bug auditado.

## Pruebas Revisadas

- [x] Revision estatica de `ComprobanteService`.
- [x] Busqueda de casts directos relacionados para descartar que el bug auditado siguiera en `ComprobanteService`.
- [x] `mvn -q -pl sginmo-web -am clean package` ejecutado desde `Desarrollo` con resultado EXIT 0.

## Pruebas Faltantes

- [ ] Prueba manual de `/comprobantes.xhtml` con cobros existentes.
- [ ] Prueba manual de reimpresion PDF de un recibo.
