# REQ-0054 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0054
- Tipo de cambio: BD (ALTER + pantalla) + backend + UI
- Riesgo: medio (estado documental de contratos/pagares; anulacion; sin tocar la generacion de REQ-0041)
- Archivos clave:
  - `V35__documento_estado.sql`: extiende `documento_generado` (estado_documental + fechas + adjunto_firmado + anulacion) + registra pantalla `documentos-generados`. La tabla ya tiene RLS (V29).
  - `dominio/documento/DocumentoGenerado.java`: nuevos campos de estado/fechas/anulacion.
  - `servicio/DocumentoGeneradoService.java`: @AislarTenant; listar/contar (lazy, filtro estado + pendientes de firma), cambiarEstado (sella fecha), registrarFirma (adjunto + FIRMADO), anular (motivo+usuario, permiso INACTIVAR). Todo bajo RLS por tenant.
  - `web/DocumentoGeneradoBean.java` + `webapp/documentos-generados.xhtml`: lista + dialogo de gestion (avanzar estado, registrar firma, anular).
  - `WEB-INF/plantilla.xhtml`, `tools/smoke-test-vps.py`: menu + cobertura.
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - V35 en `BEGIN...ROLLBACK`: 4 columnas nuevas + pantalla OK.
  - Deploy + Flyway V35 `success=t`; `python tools/smoke-test-vps.py`: 22/22 RENDER OK incl. `documentos-generados`.
- Cambios de datos: si, V35 (ALTER ADD COLUMN IF NOT EXISTS con default GENERADO; sin tocar filas de negocio).
- Cambios de entorno: no.
- Impacto LLM/tokens: no.
- Decision esperada: revisar riesgo puntual (estado/anulacion de documentos legales) + limitaciones documentadas.
- Notas para auditor:
  - Regla "anular no borra archivo ni historial": anular solo cambia estado + registra motivo/usuario/fecha; no toca el archivo ni la fila original.
  - Regla "un documento firmado no se regenera silenciosamente": este REQ NO modifica la generacion (REQ-0041); el estado FIRMADO queda como control. La proteccion en la regeneracion es responsabilidad del flujo de REQ-0041 (fuera de alcance de este REQ, notado).
  - Permisos: cambiar estado exige EDITAR; anular exige INACTIVAR.

## Resumen Funcional

Nueva pantalla "Documentos generados": lista los contratos/pagares generados con su estado documental
(GENERADO/IMPRESO/ENVIADO/FIRMADO/OBSERVADO/ANULADO/ARCHIVADO), permite avanzar el estado (sellando la
fecha), registrar la version firmada (adjunto de REQ-0053), anular con motivo, y filtrar por estado o
"pendientes de firma".

## Resumen Tecnico

Se extiende `documento_generado` (V35) con estado + fechas + anulacion + adjunto_firmado. El servicio
@AislarTenant expone listado lazy + transiciones de estado con permiso backend. Estados varchar+CHECK.

## Limitaciones Conocidas (transparencia)

- "Resumen documental en la operacion" (contrato generado/firmado, pagares generados/firmados dentro del
  ABM de Operacion): DIFERIDO. La pantalla de Documentos generados ya permite ver/filtrar por operacion y
  estado; incrustar el resumen en el ABM de Operacion es un refinamiento pendiente.
- Filtro "vencidos" por contrato (operacion.fecha_fin_contrato): refinamiento; hoy se filtra por estado y
  "pendientes de firma".

## Archivos Modificados

Ver Manifiesto. V35 nueva.

## Cambios De Datos

V35: ALTER `documento_generado` (columnas de estado/fechas/anulacion, default GENERADO) + pantalla.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; V35 validada en rollback; deploy + Flyway success; smoke 22/22. Ver test-plan.

## Pruebas Manuales Sugeridas

1. Con una operacion que ya genero un contrato (REQ-0041): abrir Documentos generados -> Gestionar -> marcar Impreso/Enviado/Firmado, anular con motivo.
2. Filtrar "solo pendientes de firma".

## Riesgos Conocidos

- Estado de documentos legales: revisar transiciones. Ver "Limitaciones Conocidas".
