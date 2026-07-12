# REQ-0061 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0061
- Tipo de cambio: BD (tabla + RLS + pantalla) + backend + UI
- Riesgo: medio (importa datos; mitigado: atomico + reusa validaciones del service)
- Archivos clave:
  - `V42__importacion.sql`: tabla `importacion` (historial, por-tenant, RLS) + pantalla `importacion`.
  - `servicio/ImportacionService.java`: @AislarTenant; framework CSV generico -> parse, preview (valida por fila), importar (ATOMICO: no inserta si hay errores), plantilla, historial. Importador PARAMETRO reusa ParametroService.guardar.
  - `web/ImportacionBean.java` + `webapp/importacion.xhtml`: plantilla + upload + preview + confirmar + historial.
  - `WEB-INF/plantilla.xhtml`, `tools/smoke-test-vps.py`: menu + cobertura.
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - V42 en `BEGIN...ROLLBACK`: tabla + 4 RLS + pantalla.
  - Deploy + Flyway V42 `success=t`; `python tools/smoke-test-vps.py`: 27/27 RENDER OK incl. `importacion`.
- Cambios de datos: si, V42 (tabla nueva + pantalla). La importacion en si inserta segun el tipo.
- Cambios de entorno: no.
- Impacto LLM/tokens: no.
- Decision esperada: revisar atomicidad + reutilizacion de validaciones + limitaciones.
- Notas para auditor:
  - Atomico: importar() valida TODAS las filas; si hay ≥1 error, no inserta ninguna (registra historial FALLIDO). Si un insert falla, el @Transactional revierte todo.
  - Reusa validaciones: PARAMETRO -> ParametroService.guardar (unicidad, tenant, reglas).
  - "No reemplaza el ETL Firebird": es una herramienta operativa; documentado.

## Resumen Funcional

Nueva pantalla "Importacion": elegir tipo, descargar plantilla CSV, subir un CSV UTF-8, ver una vista
previa con validacion por fila (OK/ERROR) y confirmar. La importacion es atomica (no inserta parcial).
Historial de importaciones (usuario, fecha, archivo, validas, errores, resultado).

## Resumen Tecnico

ImportacionService @AislarTenant parsea CSV, valida por fila y confirma atomico reusando el service de
negocio del tipo. Historial en tabla `importacion`. Tipo de referencia: PARAMETRO.

## Limitaciones Conocidas (transparencia)

- XLSX: DIFERIDO (requiere aprobar Apache POI; no esta en el pom). Hoy CSV UTF-8.
- Tipos personas/articulos/activos/propietarios: el framework es generico; cada tipo es un mapper
  pluggable (validar/insertar). Se implemento PARAMETRO como referencia; los demas mappers son el
  follow-on incremental (reusando su service de negocio). Documentado, no bloqueante.
- Parser CSV: minimo (sin comas embebidas en campos entrecomillados); documentado en el service.

## Archivos Modificados

Ver Manifiesto. V42 nueva.

## Cambios De Datos

V42: tabla `importacion` (historial) + pantalla. Las cargas dependen del CSV del usuario.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; V42 rollback OK; deploy + Flyway success; smoke 27/27. Ver test-plan.

## Pruebas Manuales Sugeridas

1. Importacion -> tipo PARAMETRO -> descargar plantilla -> completar 2 filas (una invalida) -> previsualizar (ve el error) -> confirmar (no inserta por atomico) -> corregir -> confirmar (OK).

## Riesgos Conocidos

- Importa datos: mitigado por atomicidad + reuso de validaciones. Ver "Limitaciones".
