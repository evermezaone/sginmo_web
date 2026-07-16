# REQ-0090 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-14
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Revision Estatica

- `PortalService.resumen()` ya no castea `fecha_vencimiento` a `java.sql.Date`; usa `aLocalDate()` y cubre `LocalDate`, `java.sql.Date` y `Timestamp`.
- `PortalService` aplica el helper tambien a cuotas, pagos, operaciones y liquidaciones del portal.
- `PortalTransferenciaService` aplica el helper a `ocr_fecha`, `fecha_transferencia` y `movimiento_bancario_importado.fecha`, que son columnas `date`.
- `MoraService.carteraVencida()` usa helper para `cronograma_cuota.fecha_vencimiento`.
- `ObjetivoService` usa helper para fechas de medicion y vigencias.
- Busqueda exacta de casts duros `(java.sql.Date)` en codigo Java: 0 coincidencias.

## Hallazgos

### Bloqueantes

- Ninguno.

### No Bloqueantes

- `PortalTransferenciaService.fila()` mantiene un mapeo manual de `portal_pago_transferencia.fecha`; esa columna es `timestamptz`, no `date`, y cubre `Timestamp`, `LocalDateTime` y `OffsetDateTime`. No corresponde al bug de `LocalDate` en columnas `date`.

## Riesgos

- Bajo. El cambio es defensivo en lectura de resultados nativos y no modifica esquema ni reglas de negocio.

## Pruebas Revisadas

- [x] Revision estatica de `PortalService`.
- [x] Revision estatica de `PortalTransferenciaService`.
- [x] Revision estatica de `MoraService`.
- [x] Revision estatica de `ObjetivoService`.
- [x] Revision de tipos de columnas `date`/`timestamptz` en migraciones relevantes.
- [x] Busqueda exacta de `(java.sql.Date)`: 0 coincidencias.
- [x] `mvn -q -pl sginmo-web -am clean package` EXIT 0.

## Pruebas Faltantes

- [ ] Prueba manual de portal con CI/RUC + OTP para confirmar `portal/inicio.xhtml` en entorno real.
