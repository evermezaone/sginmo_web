# REQ-0094 - Auditoria Codex

**Estado:** EN_AUDITORIA_CODEX
**Fecha:** 2026-07-14
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Hallazgos

### Bloqueantes

- Ninguno. Las observaciones bloqueantes previas quedaron cerradas:
  - Obs 316: existe auto-aplicacion gated por caja/permiso/documento/moneda/saldo, con `CajaService.cobrar()` y `portal_pago_qr.estado='APLICADO'` cuando aplica.
  - Obs 317: `intentarConciliar()` valida `expira_en`.
  - Obs 318: el movimiento bancario se marca `CONCILIADO`, por lo que deja de ser candidato para otra transferencia.

### No Bloqueantes

- Ninguno.

## Riesgos

- La auto-aplicacion es condicional ("gated"): si falta caja, permiso, documento, moneda o saldo suficiente, el intento queda `CONCILIADO` para operador. Esto está documentado como comportamiento de seguridad operativa y no rompe el criterio porque evita cobros automáticos ambiguos.

## Pruebas Revisadas

- [x] Revision estatica de `QrPagoService`, `PortalTransferenciaService`, `transferencias.xhtml` y migracion V60.
- [x] Revision de respuesta a Obs 316/317/318 en `preaudit-checklist.md`.
- [x] Build local: `mvn -q -f Desarrollo/pom.xml -pl sginmo-web -am clean package` EXIT 0.

## Pruebas Faltantes

- [ ] Prueba manual end-to-end en VPS con QR habilitado y movimiento bancario real/simulado.
