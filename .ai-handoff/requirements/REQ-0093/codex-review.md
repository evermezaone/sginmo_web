# REQ-0093 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-14
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

1. `QrPagoService.habilitado()` permite mostrar el QR si `PORTAL_QR_HABILITADO=true` y `PORTAL_QR_CUENTA` no esta vacio, pero no exige `PORTAL_QR_GUI`. El propio servicio define el payload EMVCo/SIPAP con tag 26 compuesto por subtag `00` = GUI/esquema/banco y subtag `01` = cuenta. Si `PORTAL_QR_GUI` queda vacio, el sistema igual renderiza un QR con merchant account template incompleto. Eso rompe el criterio "QR escaneable por apps bancarias de plaza" y puede mostrar al socio un QR visualmente valido pero bancariamente inutil. Debe bloquearse la habilitacion/generacion si falta el identificador GUI requerido por el estandar elegido, o documentar/implementar otro formato validado por banco.

### No Bloqueantes

- El QR queda detras de parametros (`PORTAL_QR_HABILITADO`, cuenta, merchant, ciudad, MCC, moneda, pais).
- La imagen se genera con ZXing como `data:image/png;base64`.
- El portal muestra monto, QR e instrucciones, y ofrece el boton "Ya transferi - informar transferencia".
- El flujo no aplica pagos automaticamente en Fase 1; enlaza con informar transferencia.

## Riesgos

- Activar QR con parametros incompletos haria que el usuario crea que puede pagar, pero su app bancaria podria rechazar el codigo.

## Pruebas Revisadas

- [x] Revision estatica de `QrPagoService`.
- [x] Revision estatica de `PortalBean.calcularQr()`.
- [x] Revision estatica de `portal/inicio.xhtml`.
- [x] Revision estatica de `V59__portal_qr_pago.sql`.
- [x] Revision estatica de dependencias ZXing en `pom.xml`.
- [x] `mvn -q -f Desarrollo/pom.xml -pl sginmo-web -am clean package` EXIT 0.

## Pruebas Faltantes

- [ ] Prueba manual con QR configurado con datos reales de banco/SIPAP y escaneo desde app bancaria local.
