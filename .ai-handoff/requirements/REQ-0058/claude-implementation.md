# REQ-0058 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0058
- Tipo de cambio: backend + UI + BD (solo pantalla)
- Riesgo: bajo-medio (solo lectura; genera PDF desde datos persistidos)
- Archivos clave:
  - `V39__pantalla_comprobantes.sql`: registra pantalla `comprobantes`.
  - `servicio/ComprobanteService.java`: @AislarTenant; reciboCobro(cobroId) con OpenPDF (reutiliza PdfService, sin Jasper); cobrosRecientes(estado). Formato es-PY.
  - `web/ComprobanteBean.java` + `webapp/comprobantes.xhtml`: lista de cobros + descarga/reimpresion del recibo (StreamedContent PDF).
  - `WEB-INF/plantilla.xhtml`, `tools/smoke-test-vps.py`: menu + cobertura.
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - V39 en `BEGIN...ROLLBACK`: pantalla registrada.
  - Deploy + Flyway V39 `success=t`; `python tools/smoke-test-vps.py`: 25/25 RENDER OK incl. `comprobantes`.
- Cambios de datos: si, V39 (solo pantalla).
- Cambios de entorno: no.
- Impacto LLM/tokens: no.
- Decision esperada: aprobar (riesgo bajo-medio) + revisar diferidos.
- Notas para auditor:
  - Sin JasperReports: reutiliza `PdfService` (OpenPDF, REQ-0026). No se agrega dependencia ni .jrxml.
  - "No generar si la transaccion no fue persistida": el recibo se genera desde el cobro ya persistido (em.find/native por id).
  - Reimpresion trazable: se regenera desde el cobro inmutable; el encabezado lleva numero, empresa, "Emitido" fecha/hora y usuario.
  - Permisos: VER (lista) + EXPORTAR (generar/descargar/reimprimir el PDF).

## Resumen Funcional

Nueva pantalla "Comprobantes": lista de cobros con descarga/reimpresion del recibo de cobro en PDF
(cliente, forma de pago, moneda, monto, detalle, cajero, empresa, usuario y fecha/hora de emision).

## Resumen Tecnico

ComprobanteService @AislarTenant arma el recibo con PdfService (OpenPDF) desde el cobro + joins
(v_persona, forma_pago, moneda) + cobro_detalle. Descarga como StreamedContent perezoso.

## Limitaciones Conocidas (transparencia)

- PDF de egreso/ingreso manual y de liquidacion de propietario: DIFERIDOS. Mismo patron de ComprobanteService,
  se agregan en una iteracion siguiente (o junto al modulo correspondiente).
- PDF de arqueo/cierre de caja: DIFERIDO a REQ-0059 (arqueo).
- Sucursal en el encabezado del comprobante: refinamiento (hoy: empresa + usuario + fecha/hora + numero).
- Plantillas configurables del comprobante: hoy formato estandar via PdfService; parametrizacion es REQ-0060.

## Archivos Modificados

Ver Manifiesto. V39 nueva.

## Cambios De Datos

V39: registra pantalla `comprobantes`. Sin cambios de datos de negocio.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; V39 rollback OK; deploy + Flyway success; smoke 25/25. Ver test-plan.

## Pruebas Manuales Sugeridas

1. Con cobros registrados: abrir Comprobantes -> descargar el recibo PDF de un cobro; reimprimir el mismo.

## Riesgos Conocidos

- Solo lectura; genera PDF desde datos persistidos. Ver "Limitaciones".
