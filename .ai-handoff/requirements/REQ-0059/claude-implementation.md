# REQ-0059 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0059
- Tipo de cambio: BD (ALTER planilla + pantalla) + backend + UI
- Riesgo: medio-alto (caja/dinero; se EXTIENDE la planilla existente sin tocar CajaService)
- Archivos clave:
  - `V40__caja_arqueo.sql`: ALTER `planilla` (efectivo_esperado/contado, diferencia, observacion_cierre, reabierta + reapertura) + pantalla `arqueo`.
  - `dominio/operacion/Planilla.java`: nuevos campos de arqueo (getters/setters). Sin cambiar el mapeo existente.
  - `servicio/ArqueoService.java`: @AislarTenant; resumen (totales por forma de pago + efectivo esperado), cerrarConArqueo (sella diferencia, cierra), reabrir (permiso REACTIVAR + motivo + audit), arqueoPdf (OpenPDF), planillasRecientes.
  - `web/ArqueoBean.java` + `webapp/arqueo.xhtml`: lista de planillas + arqueo/cierre + reapertura + PDF.
  - `WEB-INF/plantilla.xhtml`, `tools/smoke-test-vps.py`: menu + cobertura.
  - Fix transversal: se agrego `type="localDate"/"localDateTime"` a `f:convertDateTime` en TODAS las pantallas nuevas (agenda, documentos, documentos-generados, cobranza, comprobantes, arqueo, portal) — bug latente que fallaba al renderizar fechas con datos reales.
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - V40 en `BEGIN...ROLLBACK`: 5 columnas nuevas + pantalla OK.
  - Deploy + Flyway V40 `success=t`; `python tools/smoke-test-vps.py`: 26/26 RENDER OK incl. `arqueo` (con planillas reales).
- Cambios de datos: si, V40 (ALTER planilla ADD COLUMN IF NOT EXISTS; sin tocar filas existentes).
- Cambios de entorno: no.
- Impacto LLM/tokens: no.
- Decision esperada: revisar reglas de caja (no se rompe el modulo) + limitaciones.
- Notas para auditor:
  - "No romper el modulo existente": CajaService (abrir/cobrar/anular/cerrar) NO se modifica; el arqueo es un servicio aparte que solo agrega el cierre controlado. Smoke 26/26 (caja renderiza).
  - Atomicidad: cerrarConArqueo y reabrir son @Transactional.
  - "Anulacion post-cierre": el camino trazable es la reapertura (REACTIVAR + motivo + audit). No se toca anularCobro (critico); bloquear anular tras cierre es refinamiento de politica.

## Resumen Funcional

Nueva pantalla "Arqueo de caja": lista de planillas; para la ABIERTA, un dialogo muestra totales por
forma de pago y efectivo esperado, pide el efectivo contado y cierra con confirmacion (sella la
diferencia). Reapertura excepcional con permiso + motivo (auditada). Arqueo PDF (OpenPDF).

## Resumen Tecnico

Se extiende `planilla` (V40) con datos de arqueo. ArqueoService @AislarTenant calcula el resumen y hace
el cierre/reapertura; el PDF reutiliza PdfService. Estados de planilla ABIERTA/CERRADA (existentes).

## Limitaciones Conocidas (transparencia)

- Conteo por denominacion: DIFERIDO; se registra el monto contado (decision de alcance).
- Diferencia "antes de cerrar" en vivo: se muestra el esperado; la diferencia se sella al confirmar.
- Estado de caja en el dashboard gerencial (0056): refinamiento; hoy visible en la pantalla de arqueo.
- Bloquear anular cobro tras el cierre (sin reabrir): refinamiento de politica; hoy la reapertura es el camino trazable.

## Archivos Modificados

Ver Manifiesto. V40 nueva; fix transversal de convertDateTime.

## Cambios De Datos

V40: ALTER `planilla` (columnas de arqueo/reapertura) + pantalla `arqueo`.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; V40 rollback OK; deploy + Flyway success; smoke 26/26 (arqueo con datos reales). Ver test-plan.

## Pruebas Manuales Sugeridas

1. Con una planilla ABIERTA y cobros: abrir Arqueo -> ver totales por forma de pago + esperado -> ingresar contado -> cerrar. Descargar arqueo PDF. Reabrir con motivo.

## Riesgos Conocidos

- Toca el dominio de caja (pero no modifica CajaService). Ver "Limitaciones".
