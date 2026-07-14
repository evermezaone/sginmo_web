# REQ-0094 - Implementacion (base de Fase 2: QR dinamico + auto-match)

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-14
**Rama:** multiempresa

## Alcance entregado (decision del usuario: "base ahora, auto-apply despues")

Se desarrollo el nucleo DESARROLLABLE y TESTEABLE de la Fase 2:
- **QR dinamico**: cada QR persiste una intencion de pago con una **referencia UNICA** (reusa una PENDIENTE
  no vencida del mismo importe para no duplicar por carga de pagina).
- **Auto-match**: al importar/registrar un movimiento bancario (REQ-0085), se concilia automaticamente el
  intento QR PENDIENTE cuya referencia aparece en la del movimiento y cuyo importe coincide (UPDATE atomico).
- **Visibilidad para el operador**: panel "Pagos por QR conciliados (listos para aplicar)" en la bandeja.

**Queda pendiente (bloqueo externo/negocio, documentado):** la aplicacion 100% automatica del cobro
(imputacion a cuota/planilla) y la confirmacion en tiempo real via webhook/API del banco o PSP. Requieren
convenio/credenciales y una regla de imputacion. Hoy el operador aplica el cobro por el flujo habitual.

## Manifiesto Minimo Para Codex

- REQ: REQ-0094
- Tipo de cambio: backend + UI + BD (migracion)
- Riesgo: bajo-medio (feature aislada; no aplica cobros automaticamente)
- Archivos clave:
  - `db/migration/V60__portal_qr_intento.sql`: tabla portal_pago_qr (RLS + referencia unica) + param PORTAL_QR_INTENTO_EXPIRA_MIN.
  - `servicio/QrPagoService.java`: referenciaIntento() (get-or-create) + intentarConciliar() (UPDATE atomico) + reference generator.
  - `servicio/PortalTransferenciaService.java`: registrarMovimiento/importarCsv ahora RETURNING id y llaman intentarConciliar; intentosQrConciliados() (lectura para el operador).
  - `web/PortalBean.java`: el QR usa la referencia dinamica del intento (fallback a documento).
  - `web/TransferenciaBandejaBean.java` + `webapp/transferencias.xhtml`: panel de pagos QR conciliados.
- Comandos probados:
  - `python xml.dom.minidom.parse` transferencias.xhtml/inicio.xhtml: OK.
  - `powershell -File tools/deploy-vps.ps1` (mvn + migrate V60 + deploy): Build OK / Redeploy OK, EXIT: 0.
  - `python tools/smoke-test-vps.py`: === RESULTADO: TODAS OK === (incluye render de transferencias con el panel QR).
- Cambios de datos: si (V60: tabla portal_pago_qr + 1 parametro; idempotente)
- Cambios de entorno: no
- Decision esperada: aprobar (base de Fase 2); el auto-apply + webhook quedan como extension pendiente de convenio.
- Notas para auditor: el auto-match es UPDATE...RETURNING atomico por (tenant, estado PENDIENTE, importe,
  referencia contenida). RLS por tenant en portal_pago_qr. No se aplican cobros sin intervencion.

## Resumen Funcional

El QR de pago ahora es dinamico (referencia unica por pago). Cuando entra el movimiento bancario, el sistema
lo cruza solo y marca el pago como conciliado; el operador lo ve en un panel y lo aplica.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| `db/migration/V60__portal_qr_intento.sql` | tabla portal_pago_qr (RLS, referencia unica) + param expira |
| `servicio/QrPagoService.java` | referenciaIntento + intentarConciliar + generador |
| `servicio/PortalTransferenciaService.java` | hook auto-match en registrar/importar + intentosQrConciliados |
| `web/PortalBean.java` | QR con referencia dinamica |
| `web/TransferenciaBandejaBean.java` | carga/getter de pagos QR conciliados |
| `webapp/transferencias.xhtml` | panel "Pagos por QR conciliados" |

## Cambios De Datos

V60: CREATE TABLE portal_pago_qr (RLS FORCE + ux por referencia) + INSERT del parametro PORTAL_QR_INTENTO_EXPIRA_MIN. Idempotente.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

- XHTML bien formados.
- Build OK (mvn clean package); migrate V60 aplicado; deploy Redeploy OK; login 200.
- smoke-test-vps.py: 37 pantallas 200 (TODAS OK) — incluye 'transferencias' con el panel QR.

## Pruebas Manuales Sugeridas

1. Con QR habilitado, entrar al portal -> el QR lleva una referencia unica (se crea la intencion).
2. En la bandeja, registrar/importar un movimiento con esa referencia y el mismo importe -> el intento pasa a CONCILIADO y aparece en el panel.

## Riesgos Conocidos

Auto-aplicacion del cobro y confirmacion en tiempo real (webhook/PSP) NO incluidas: dependen de convenio con banco/PSP y de definir la regla de imputacion (decision de negocio).
