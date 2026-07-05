# REQ-0003 - Plan De Pruebas

**Fecha:** 2026-07-05

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | Aplicar V1 en PostgreSQL 16 de la VPS (psql ON_ERROR_STOP) | sin errores | **OK** (tras fix: sin UNIQUE por nombre en ubicacion_geografica, ver incidencia 1) |
| T02 | Aplicar V2 (seed basico) | sin errores; setval correctos | **OK** |
| T03 | Aplicar V3 (ubicaciones INE) | sin errores | **OK** |
| T04 | Conteos de verificacion | 36 tablas, 1 vista, entidad=84, ubicaciones=8276, articulos=15, parametros=7 | **OK** — exactos |
| T05 | Jerarquia geografica navegable | barrios de Asuncion via join por padre y codigo_oficial '0000' | **OK** — 68 barrios |
| T06 | Validacion fuente INE (pre-aplicacion) | 0 codigos duplicados en 7.994 barrios | **OK** (700 filas descartadas eran separadores vacios del XLSX) |

## Pruebas Manuales

| ID | Escenario | Pasos | Resultado esperado | Resultado real |
|---|---|---|---|---|
| M01 | Revision del esquema por el usuario | 11 observaciones en 6 revisiones interactivas | aprobacion explicita | **OK** — "La base queda aprobada en su version 1" (2026-07-05) |

## Incidencias detectadas y resueltas

1. V3 fallo en el primer intento: UNIQUE(padre,nombre,nivel) chocaba con barrios homonimos reales del INE (dos "Ciudad Nueva" con codigos distintos). Fix: se quito ese UNIQUE del V1 (la unicidad real es codigo_oficial) y se reaplico todo desde esquema limpio (DROP SCHEMA — BD sin datos de valor).

## Datos De Prueba

BD `sginmo` en la VPS (127.0.0.1:5432 alla). Credenciales en `.env` (APP_DB_*), no versionadas.
