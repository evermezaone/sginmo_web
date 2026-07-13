# REQ-0087 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-13
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0087
- Tipo de cambio: BD (V55) + entidad Activo + ActivoBean + activos.xhtml.
- Riesgo: medio-alto (reestructura del formulario de activos).
- Archivos clave:
  - `V55__activo_lotes_casas.sql`: superficie, dimensiones_linderos, cochera (CHECK 0..10), m2_construccion,
    medida, ande_medidor/ande_nis, essap_medidor/essap_cta_cte (todas opcionales).
  - `dominio/activo/Activo.java`: campos + getters/setters.
  - `web/ActivoBean.java`: `generarTipo` (LOTES/CASAS_DPTOS); helpers de modo (esNuevo/esLote/modoGenerarLotes/
    modoLoteDetalle/modoCasaDpto); `tiposFormulario` (excluye Lote/Loteamiento en casa/dpto); `cocheras` (1..10);
    `guardarDialogo()` (alta LOTES -> generarLotes; si no -> guardar). `generarLotes()` ahora oculta ambos dialogos.
  - `webapp/activos.xhtml`: combo GENERAR (solo alta, ajax update del form); fragmento de generacion masiva
    embebido (rendered modoGenerarLotes); tabView rendered cuando NO es generacion masiva; secciones condicionales
    de "Datos del lote" (modoLoteDetalle) y "Datos inmobiliaria/servicios" (modoCasaDpto); Tipo filtrado por
    tiposFormulario y bloqueado en lote-detalle; boton unico Guardar/Generar -> guardarDialogo().
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - Deploy VPS + Flyway V55 (schema v55); `python tools/smoke-test-vps.py`: 36/36 (activos 200).
- Cambios de datos: si, V55 (9 columnas opcionales). Cambios de entorno: no.
- Decision esperada: aprobar; ver "Follow-up" en req.md (adjuntos inline y combos dedicados quedan como REQ aparte).
- Notas para auditor:
  - En generacion masiva no se renderiza el tabView, por lo que los required (Nombre/Tipo) no aplican en ese modo.
  - Persistencia por el merge/persist existente de ActivoService (sin cambios de servicio).
  - Reutiliza cascada de ubicacion, precios, catastro, observacion y el contenedor "Caracteristicas" (padre).

## Resumen Funcional

En Activos, el combo GENERAR alterna generacion masiva de lotes vs. carga de casa/dpto; al editar un lote se ve su
formulario detallado; casa/dpto suma cochera, m2, medida y servicios ANDE/ESSAP.

## Resumen Tecnico

Combo de modo + fragmentos condicionales en el dialogo de activo, con 9 columnas nuevas persistidas por JPA.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| V55__activo_lotes_casas.sql | NUEVO - 9 columnas |
| dominio/activo/Activo.java | campos + getters/setters |
| web/ActivoBean.java | combo GENERAR, modos, tiposFormulario, cocheras, guardarDialogo |
| webapp/activos.xhtml | combo GENERAR + fragmentos condicionales + secciones lote/casa + servicios |

## Cambios De Datos

V55: superficie, dimensiones_linderos, cochera, m2_construccion, medida, ande_*, essap_* en activo.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; deploy VPS; Flyway V54+V55 (schema v55); smoke 36/36 (activos 200).

## Pruebas Manuales Sugeridas

1. Nuevo -> GENERAR=LOTES -> completar loteamiento/manzana/cantidad -> Generar -> se crean lotes.
2. Nuevo -> GENERAR=CASAS/DPTOS -> Tipo (sin Lote/Loteamiento) + cochera/m2/medida + ANDE/ESSAP -> Guardar -> reeditar.
3. Editar un lote -> ver "Datos del lote" (superficie/dimensiones) + tipo fijo Lote -> Guardar -> reeditar.

## Limitaciones Conocidas

- Adjuntos inline y combos dedicados de loteamiento/complejo quedan como follow-up (ver req.md).

## Riesgos Conocidos

- Reestructura de UI: mitigada reutilizando el form existente y con render condicional por modo; smoke 200.
