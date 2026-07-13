# REQ-0088 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-13
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0088
- Tipo de cambio: BD (V54) + entidad Activo + UI (activos.xhtml). Sin cambios de servicio (JPA merge existente).
- Riesgo: bajo (campos nuevos opcionales + label).
- Archivos clave:
  - `V54__activo_campos_operacion.sql`: agrega a `activo` las columnas `tipo_operacion` (CHECK ALQUILER/VENTA,
    nullable), `medidas` varchar(120), `anio` integer, `cantidad_unidades` integer.
  - `dominio/activo/Activo.java`: campos + getters/setters (persisten via el merge/persist ya existente en ActivoService).
  - `webapp/activos.xhtml` (tab Datos): combo Operacion (Alquiler/Venta), Medidas, Anio, Cantidad de unidades; y el
    autoComplete de contenedor renombrado de "Contenedor (edificio/loteamiento - opcional)" a "Caracteristicas".
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - Deploy VPS + Flyway V54; `python tools/smoke-test-vps.py`: 36/36 (activos 200).
- Cambios de datos: si, V54 (4 columnas). Cambios de entorno: no.
- Decision esperada: aprobar; campos opcionales que no rompen datos existentes.
- Notas para auditor:
  - CHECK de tipo_operacion admite NULL (activos existentes no se rompen).
  - Sin cambios en ActivoService: la entidad se persiste por merge/persist ya vigente.

## Resumen Funcional

El formulario de activos suma Operacion (Alquiler/Venta), Medidas, Anio y Cantidad de unidades, y el contenedor
pasa a llamarse "Caracteristicas".

## Resumen Tecnico

4 columnas nuevas en activo + campos en la entidad + controles en la pestana Datos de activos.xhtml.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| V54__activo_campos_operacion.sql | NUEVO - tipo_operacion/medidas/anio/cantidad_unidades |
| dominio/activo/Activo.java | 4 campos + getters/setters |
| webapp/activos.xhtml | 4 controles nuevos + rename label a "Caracteristicas" |

## Cambios De Datos

V54: columnas tipo_operacion (CHECK), medidas, anio, cantidad_unidades en activo.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; deploy VPS; Flyway V54; smoke 36/36 (activos 200).

## Pruebas Manuales Sugeridas

1. Nuevo activo: cargar Operacion/Medidas/Anio/Cantidad -> guardar -> reeditar y verificar que persistieron.
2. Verificar que la etiqueta dice "Caracteristicas".

## Limitaciones Conocidas

- Ninguna. (La reestructura mayor del formulario es REQ-0087.)

## Riesgos Conocidos

- Ninguno relevante (campos opcionales).
