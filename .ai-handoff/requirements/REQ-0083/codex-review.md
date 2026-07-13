# REQ-0083 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-13
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Reauditoria De Observaciones

### Obs 304 - Concurrencia / doble aplicacion

**Estado:** corregida.

- `PortalTransferenciaService#aprobar()` ahora reclama la fila antes de invocar `cajaService.cobrar(...)` mediante `UPDATE portal_pago_transferencia SET estado='EN_REVISION' WHERE portal_pago_transferencia=:id AND estado IN (...) RETURNING ...`.
- Si otro request ya cerro o esta procesando la transferencia, el `RETURNING` queda vacio y falla antes de generar cobro.
- El cierre final a `APLICADO` esta condicionado a `estado='EN_REVISION'`.
- La operacion sigue dentro de `@Transactional`, por lo que el lock de escritura del `UPDATE` se mantiene hasta commit.

### Obs 305 - Validacion de archivo

**Estado:** corregida.

- `informar()` detecta el tipo real con `firmaContenido(byte[])`.
- Se validan firmas de PDF, JPEG, PNG y WEBP.
- La extension/MIME declarado se cruza contra la firma detectada y se rechaza si no coincide.
- El archivo se guarda con la extension canonica detectada por contenido.

## Validacion Realizada

- Revision estatica de `PortalTransferenciaService`.
- Revision de `V56__portal_pago_transferencia.sql`.
- Revision de extensiones introducidas por `V57`/`V58` porque el servicio actual ya contiene codigo de OCR/conciliacion.
- Verificacion de `AUDITORIA_OBSERVACION`: observaciones 304 y 305 en estado `corregida`.
- Build: `mvn -q -pl sginmo-web -am clean package` desde `Desarrollo`, resultado EXIT 0.

## Hallazgos

### Bloqueantes

- Ninguno.

### No Bloqueantes

- `preaudit-checklist.md` quedo desactualizado porque todavia indica "sin observaciones previas"; la BD y el codigo real si reflejan las correcciones, por lo que no bloquea la aprobacion.
- La Fase 1 excluye OCR y conciliacion bancaria por descomposicion explicita a `REQ-0084` y `REQ-0085`.
- La descarga del recibo desde el portal queda documentada como follow-up menor; el alcance de Fase 1 acepta que el recibo quede disponible internamente.
