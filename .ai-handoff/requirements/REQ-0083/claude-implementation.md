# REQ-0083 (Fase 1) - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-13
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0083 (Fase 1 de la plataforma de auto-pago por transferencia; ver plan en analysis.md).
- Tipo de cambio: BD (V56) + servicio + 2 beans + 2 xhtml + menu + smoke. Reutiliza el motor de cobros.
- Riesgo: medio-alto (aplica cobros / maneja dinero + archivos externos).
- Archivos clave:
  - `V56__portal_pago_transferencia.sql`: tabla `portal_pago_transferencia` (estado, importe, moneda, datos
    bancarios, numero_transaccion, documento, motivo_revision, cobro, comprobante inline [nombre/fisico/mime/
    hash/tamano], columnas reservadas OCR/conciliacion nullable) con RLS por tenant; unique index parcial
    anti-doble-aplicacion sobre (tenant, numero_transaccion) WHERE estado='APLICADO'; params PORTAL_TRANSF_*;
    pantalla `transferencias`.
  - `servicio/PortalTransferenciaService.java` (@AislarTenant): informar (valida tipo/tamano, hash SHA-256,
    guarda fuera del webroot, inserta RECIBIDO), mias(persona), bandeja(estado), observar/rechazar (motivo),
    aprobar (aplica el cobro con CajaService.cobrar forma TRANSFERENCIA: cuenta=cuenta_origen, referencia=
    numero_transaccion, contra el documento elegido y la planilla abierta; marca APLICADO + cobro), descargar.
  - `web/PortalTransferenciaBean.java` + `webapp/portal/transferencia.xhtml`: el socio informa la transferencia
    (importe, fecha, banco, cuenta, nro, obs) + adjunta comprobante (p:fileUpload) y ve sus transferencias/estados.
    Link desde portal/inicio (solo cliente).
  - `web/TransferenciaBandejaBean.java` + `webapp/transferencias.xhtml`: bandeja interna con filtro por estado,
    ver/descargar comprobante, y dialogo de revision (elegir documento+emisor -> Aprobar y aplicar; u Observar/
    Rechazar con motivo). Menu en plantilla.xhtml. `transferencias` en el smoke.
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - Deploy VPS + Flyway V56 (schema v56, tras corregir tipo de parametro STRING); `python tools/smoke-test-vps.py`:
    37/37 (transferencias 200; portal/transferencia protegido 302).
- Cambios de datos: si, V56 (tabla + RLS + params + pantalla). Cambios de entorno: no.
- Decision esperada: aprobar Fase 1; OCR (0084) y conciliacion bancaria (0085) van en las fases siguientes.
- Notas para auditor:
  - Aislamiento: el socio solo ve/crea sus transferencias (RLS por tenant + persona de PortalSesion, REQ-0078).
  - Archivo: valida extension/MIME (PDF/JPG/PNG/WEBP) + tamano (param) + hash; guardado en SGINMO_ARCHIVOS_DIR/<tenant>.
  - Aplicacion: SIEMPRE via el motor de caja (f_cobrar_documento) -> no duplica reglas de cobro; requiere caja abierta.
  - Anti-doble-aplicacion: unique index parcial por (tenant, numero_transaccion) sobre APLICADO.
  - Sin autoaplicacion: en Fase 1 la aprobacion es manual (no hay OCR ni match bancario).

## Resumen Funcional

El socio informa una transferencia y adjunta el comprobante desde el portal; queda en una bandeja interna donde
un operador la revisa y, si es valida, la aplica (genera el cobro e imputa el documento) o la observa/rechaza con
motivo. Sin OCR ni conciliacion bancaria (fases 2/3).

## Resumen Tecnico

Tabla con RLS + anti-doble-aplicacion; servicio que reutiliza el motor de cobros; portal (informar) + bandeja interna.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| V56__portal_pago_transferencia.sql | NUEVO - tabla + RLS + params + pantalla |
| servicio/PortalTransferenciaService.java | NUEVO |
| web/PortalTransferenciaBean.java + webapp/portal/transferencia.xhtml | NUEVOS (portal informar) |
| web/TransferenciaBandejaBean.java + webapp/transferencias.xhtml | NUEVOS (bandeja interna) |
| WEB-INF/plantilla.xhtml, portal/inicio.xhtml, tools/smoke-test-vps.py | menu + link + cobertura |

## Cambios De Datos

V56: portal_pago_transferencia (RLS) + params PORTAL_TRANSF_* + pantalla transferencias.

## Variables De Entorno

Usa SGINMO_ARCHIVOS_DIR (ya existente) para el almacenamiento del comprobante.

## Pruebas Ejecutadas

Build OK; deploy VPS; Flyway V56; smoke 37/37 (transferencias 200; portal protegido 302).

## Pruebas Manuales Sugeridas

1. Socio informa una transferencia con comprobante -> RECIBIDO.
2. Interno abre Transferencias -> Revisar -> documento+emisor -> Aprobar (con caja abierta) -> APLICADO + cobro.
3. Observar/Rechazar con motivo -> el socio ve el motivo.

## Limitaciones Conocidas

- Descarga del recibo del cobro DESDE el portal: follow-up menor (el recibo se genera; se ve por Comprobantes).
- Imputacion la decide el operador en la bandeja (no el cliente) en Fase 1.

## Riesgos Conocidos

- Maneja dinero: mitigado reutilizando el motor de cobros, aprobacion manual, anti-doble-aplicacion, validacion de
  archivo, RLS y auditoria.
