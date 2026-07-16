# REQ-0091 - Auditoria Codex

**Estado:** APROBADO
**Fecha:** 2026-07-14
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Verificacion

- `PortalService.pagos()` devuelve fecha, monto, moneda, canal, estado, forma, recibo y referencia bancaria cuando existe.
- El canal se deriva de la forma de pago `TRF` como `Transferencia`; el resto queda como `Caja`.
- El estado se expone en el panel con etiqueta amigable (`Confirmado` para cobros `ACTIVO`).
- La referencia de transferencia se consulta desde `dato_cobro.referencia`, cubriendo la observacion anterior.
- `portal/inicio.xhtml` muestra el panel lateral `Mis pagos` con monto/moneda, badge de canal, fecha, forma, estado, referencia y recibo cuando existen.
- El layout lateral y el apilado responsive permanecen en `WEB-INF/portal.xhtml`.
- La consulta mantiene filtro por `c.persona = :p` y el servicio conserva `@AislarTenant`.

## Pruebas Revisadas

- [x] Revision estatica de `PortalService.pagos()` y `FilaPago`.
- [x] Revision estatica de `PortalTransferenciaService.aprobar()` para validar donde se persiste la referencia de transferencia.
- [x] Revision estatica de `PortalBean`.
- [x] Revision estatica de `portal/inicio.xhtml`.
- [x] Revision estatica de CSS responsive en `WEB-INF/portal.xhtml`.
- [x] `mvn -q -f Desarrollo/pom.xml -pl sginmo-web -am clean package` EXIT 0.

## Riesgo Residual

- Falta prueba manual con CI/RUC + OTP en navegador para validar datos reales desktop/mobile, pero la implementacion cumple el alcance auditable del REQ.
