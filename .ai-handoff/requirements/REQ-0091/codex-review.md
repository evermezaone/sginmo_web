# REQ-0091 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-14
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

1. El panel lateral ya muestra estado, moneda y recibo cuando existe, pero aun no cumple completamente el alcance de "referencia/numero de comprobante si existe" para pagos por transferencia. Al aplicar una transferencia, `PortalTransferenciaService.aprobar()` pasa el numero de transaccion como referencia del cobro (`dato_cobro.referencia`) mediante `cajaService.cobrar(...)`; sin embargo `PortalService.pagos()` solo consulta `documento d ON d.documento = c.recibo_documento` y arma `pago.comprobante` desde serie/numero del recibo. Si el pago por transferencia no tiene recibo_documento o si la referencia bancaria es el dato relevante para el socio, el panel no muestra esa referencia existente. Debe consultar tambien `dato_cobro.referencia`/`numero` y exponerla como referencia/comprobante visible, con preferencia clara: recibo si existe, referencia bancaria si existe, o ambos si aportan informacion distinta.

### Corregido en esta ronda

- El estado del pago ya se muestra en `portal/inicio.xhtml` mediante `pg.estadoLabel`.
- `PortalService.FilaPago` ya expone moneda (`mo.simbolo`) y el panel la muestra junto al importe.
- El layout lateral `Mis pagos` y el apilado responsive permanecen implementados.
- El filtro principal de seguridad se mantiene por `c.persona = :p` y el servicio conserva `@AislarTenant`.

## Riesgos

- Para pagos por transferencia, el socio puede ver el canal "Transferencia" y el estado "Confirmado", pero no necesariamente el numero/referencia bancaria que permite reconocer su pago.

## Pruebas Revisadas

- [x] Revision estatica de `PortalService.pagos()` y `FilaPago`.
- [x] Revision estatica de `PortalTransferenciaService.aprobar()` para validar donde se persiste la referencia de transferencia.
- [x] Revision estatica de `PortalBean`.
- [x] Revision estatica de `portal/inicio.xhtml`.
- [x] Revision estatica de CSS responsive en `WEB-INF/portal.xhtml`.
- [x] `mvn -q -f Desarrollo/pom.xml -pl sginmo-web -am clean package` EXIT 0.

## Pruebas Faltantes

- [ ] Prueba manual de portal con CI/RUC + OTP para validar vista real desktop/mobile.
