# REQ-0093 - Auditoria Codex

**Estado:** APROBADO
**Fecha:** 2026-07-14
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Verificacion

- `QrPagoService.habilitado()` exige `PORTAL_QR_HABILITADO=true`, `PORTAL_QR_CUENTA` cargada y `PORTAL_QR_GUI` cargado.
- El payload EMVCo/SIPAP usa merchant account template `tag 26` con `subtag 00` = GUI/esquema/banco y `subtag 01` = cuenta.
- El QR incluye monto, moneda, pais, merchant, ciudad, referencia y CRC16.
- La imagen se genera con ZXing como `data:image/png;base64`.
- El portal muestra seccion "Pagar por QR", monto, imagen QR e instrucciones.
- El boton "Ya transferi - informar transferencia" lleva al flujo de REQ-0092.
- El flujo no aplica pago automaticamente en Fase 1.

## Pruebas Revisadas

- [x] Revision estatica de `QrPagoService`.
- [x] Revision estatica de `PortalBean.calcularQr()`.
- [x] Revision estatica de `portal/inicio.xhtml`.
- [x] Revision estatica de `V59__portal_qr_pago.sql`.
- [x] Revision estatica de dependencias ZXing en `pom.xml`.
- [x] `mvn -q -f Desarrollo/pom.xml -pl sginmo-web -am clean package` EXIT 0.

## Riesgo Residual

- Falta prueba manual con datos reales del banco/SIPAP y escaneo desde una app bancaria local. La implementacion ya bloquea parametros incompletos, pero la validacion final de plaza requiere credenciales/datos reales.
