# REQ-0093 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-14
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0093
- Tipo de cambio: backend + UI + BD (migracion de parametros) + dependencia nueva
- Riesgo: bajo-medio (feature nueva aislada; deshabilitada por defecto)
- Archivos clave:
  - `pom.xml`: dependencia com.google.zxing core+javase 3.5.3 (generacion de QR, Java puro).
  - `db/migration/V59__portal_qr_pago.sql`: siembra PORTAL_QR_* (tenant -1), QR DESHABILITADO por defecto.
  - `servicio/QrPagoService.java`: payload EMVCo (TLV + CRC16-CCITT) + PNG data URI (ZXing). Parametrizable.
  - `web/PortalBean.java`: calcularQr() (monto = deuda vencida, o proxima cuota) + getters qrHabilitado/qrMonto/qrDataUri.
  - `webapp/portal/inicio.xhtml`: seccion "Pagar por QR" (rendered si habilitado) con la imagen + CTA "informar transferencia".
- Comandos probados:
  - `python xml.dom.minidom.parse` inicio.xhtml/transferencia.xhtml: OK.
  - `powershell -File tools/deploy-vps.ps1` (mvn clean package + migrate V59 + deploy): Build OK / Redeploy OK, EXIT: 0.
  - `python tools/smoke-test-vps.py`: === RESULTADO: TODAS OK ===.
- Cambios de datos: si (V59, solo INSERT de parametros con NOT EXISTS; idempotente)
- Cambios de entorno: no
- Decision esperada: aprobar
- Notas para auditor: Fase 1 = QR EMVCo con monto + cuenta destino; SIN conciliacion automatica (Fase 2 =
  REQ-0094). El QR queda DESHABILITADO hasta que la empresa cargue PORTAL_QR_CUENTA y active PORTAL_QR_HABILITADO.
  El contenido del "merchant account template" (tag 26 = GUI + cuenta) se toma de parametros tal cual, sin
  asumir un formato propietario del banco/SIPAP (dato a definir por la empresa; se documenta como decision de negocio).

## Resumen Funcional

En el portal, cuando la empresa lo habilita, el socio ve un "Pagar por QR" con el codigo para transferir el
saldo (deuda vencida o proxima cuota) desde su app bancaria; luego confirma informando la transferencia.

## Resumen Tecnico

QrPagoService arma un payload EMVCo (TLV, currency 600/PYG, country PY, amount, merchant account tag 26 con
GUI+cuenta, additional data tag 62-05 = documento del socio) y calcula el CRC16-CCITT; genera el PNG con
ZXing y lo devuelve como data URI base64. PortalBean lo calcula una vez por vista para el saldo a pagar.
La UI muestra la imagen si el QR esta habilitado.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| `pom.xml` | dependencia ZXing core+javase 3.5.3 |
| `db/migration/V59__portal_qr_pago.sql` | parametros PORTAL_QR_* (deshabilitado por defecto) |
| `servicio/QrPagoService.java` | payload EMVCo + PNG (nuevo) |
| `web/PortalBean.java` | calcularQr + getters del QR |
| `webapp/portal/inicio.xhtml` | seccion "Pagar por QR" |

## Cambios De Datos

V59: INSERT (NOT EXISTS) de PORTAL_QR_HABILITADO/GUI/CUENTA/MERCHANT/CIUDAD/MCC/MONEDA/PAIS en tenant -1. Idempotente.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

- inicio.xhtml/transferencia.xhtml XML bien formado.
- Build OK (mvn clean package, incluye ZXing); migrate V59 aplicado; deploy Redeploy OK; login 200.
- smoke-test-vps.py: 37 pantallas 200 (TODAS OK).

## Pruebas Manuales Sugeridas

1. En Parametros (global -1 o empresa) cargar PORTAL_QR_CUENTA (+ GUI/MERCHANT) y PORTAL_QR_HABILITADO=true.
2. Entrar al portal como socio con saldo -> aparece "Pagar por QR" con la imagen del monto adeudado.
3. Escanear con una app bancaria compatible EMVCo/SIPAP (validar con el banco el contenido del tag 26).

## Riesgos Conocidos

El formato exacto del merchant account template (tag 26) depende del banco/SIPAP: se parametriza, no se asume.
La vista del portal no entra en el smoke (OTP).
