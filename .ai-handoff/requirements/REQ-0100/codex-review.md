# REQ-0100 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-16 09:17 -03:00
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Hallazgos

### Bloqueantes

- Ninguno.

### No Bloqueantes

- Ninguno.

## Riesgos

Ninguno identificado.

## Pruebas Revisadas

- [x] Revision estatica de `portal/transferencia.xhtml`: agrega boton visible "Volver a mi cuenta" al tope del contenido, ademas del link superior.
- [x] Revision estatica de `PortalBean`: carga `transferenciasEnProceso` desde `PortalTransferenciaService.mias(persona)` y filtra `estado != APLICADO`.
- [x] Revision estatica de `portal/inicio.xhtml`: panel "Transferencias en proceso" aparece en el aside, sobre "Mis pagos", con fecha, monto, estado y motivo cuando existe.
- [x] Revision de aislamiento: `mias(persona)` consulta por persona autenticada; no se agregaron consultas globales ni cambios de BD.
- [x] Revision de template `WEB-INF/portal.xhtml`: badge `b-proceso` agregado para estado visual.
- [x] Build local re-ejecutado el 2026-07-16: `mvn -q -f Desarrollo\pom.xml -pl sginmo-web -am clean package` EXIT 0.

## Pruebas Faltantes

- [ ] Prueba manual en portal/VPS con transferencia RECIBIDO y luego APLICADO.
