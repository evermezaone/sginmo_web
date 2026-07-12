# REQ-0054 - Auditoria Codex

**Estado:** EN_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

- `DocumentoGeneradoService.registrarFirma` acepta un `adjuntoFirmadoId` arbitrario y lo asigna directamente al documento generado sin cargar ni validar el `DocumentoAdjunto` bajo RLS/tenant actual. La FK solo valida existencia global, no pertenencia al mismo tenant ni estado ACTIVO. Con la UI actual, que pide escribir el id del adjunto escaneado, un usuario podria asociar como firmado un adjunto de otra empresa si conoce o adivina el id. Esto rompe aislamiento multiempresa y trazabilidad documental.

### No Bloqueantes

- Ninguno.

## Riesgos

- Riesgo de asociacion cruzada de adjuntos firmados entre tenants, con posible fuga posterior por pantallas/reportes que resuelvan el adjunto asociado.

## Pruebas Revisadas

- [x] Revision estatica de `V35__documento_estado.sql`
- [x] Revision estatica de `DocumentoGeneradoService`
- [x] Revision estatica de `documentos-generados.xhtml`
- [x] Build Maven previo: `mvn -q clean package` EXIT 0

## Pruebas Faltantes

- [ ] Prueba manual post-fix: intentar registrar como firmado un adjunto inexistente, inactivo y de otro tenant; debe rechazarlo con mensaje de negocio.

## Solucion Esperada

- En `registrarFirma`, si se informa `adjuntoFirmadoId`, cargar `DocumentoAdjunto` con `em.find(DocumentoAdjunto.class, adjuntoFirmadoId)` dentro de `@AislarTenant`.
- Rechazar si no existe para el tenant actual o si no esta `ACTIVO`.
- Rechazar asociaciones incompatibles con la operacion/documento cuando aplique.
- Mantener la FK como respaldo, pero no usarla como unica validacion de seguridad.

