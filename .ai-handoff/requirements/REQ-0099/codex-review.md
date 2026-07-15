# REQ-0099 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-14 21:22 -03:00
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

- [x] Revision estatica de `portal/inicio.xhtml`: el boton "Informar transferencia" esta visible dentro de la tarjeta "Mis cuotas", junto al selector de periodo y condicionado a `esCliente`.
- [x] Revision estatica de `portal/transferencia.xhtml`: el formulario de carga solo solicita `Monto` y `Comprobante`; los datos removidos quedan fuera del formulario.
- [x] Revision backend de `PortalTransferenciaBean` y `PortalTransferenciaService`: `informar()` solo exige persona, importe positivo y archivo; fecha/banco/cuenta/nro/observacion/documento son opcionales y se insertan como null.
- [x] Re-auditoria del fix posterior de subida: `p:fileUpload` avanzado con listener `subirComprobante()` guarda bytes/nombre/mime en el `ViewScoped` y `informar()` usa esos bytes, manteniendo el formulario con solo monto + comprobante.
- [x] Revision de migracion `V56__portal_pago_transferencia.sql`: los campos removidos del formulario no son `NOT NULL`; el flujo RECIBIDO/EN_REVISION/APLICADO y RLS permanecen vigentes.
- [x] Build local re-ejecutado: `mvn -q -f Desarrollo\pom.xml -pl sginmo-web -am clean package` EXIT 0.

## Pruebas Faltantes

- [ ] Prueba manual en navegador/VPS con usuario portal real.
