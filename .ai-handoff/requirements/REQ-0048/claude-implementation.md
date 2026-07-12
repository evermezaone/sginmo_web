# REQ-0048 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0048
- Tipo de cambio: UI + backend + BD (migracion de tipo de columna + siembra de catalogo)
- Riesgo: medio (migracion: cambia el tipo de `articulo.clasificacion` de varchar a bigint)
- Archivos clave:
  - `sginmo-web/src/main/java/py/com/pysistemas/sginmo/dominio/catalogo/Articulo.java`: `clasificacion` pasa de `String` a `Long` (id de entidad de la lista CLASIFICACION_ARTICULO).
  - `sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/ArticuloBean.java`: carga `clasificaciones = catalogoService.opciones("CLASIFICACION_ARTICULO")` y expone `getClasificaciones()`.
  - `sginmo-web/src/main/webapp/articulos.xhtml`: el campo deja de ser texto libre y usa `p:selectOneMenu` sobre `articuloBean.clasificaciones` (itemValue = id).
  - `sginmo-web/src/main/resources/db/migration/V31__clasificacion_articulo_lista.sql`: siembra global (tenant -1) de CLASIFICACION_ARTICULO y `ALTER TABLE articulo ALTER COLUMN clasificacion TYPE bigint`.
- Comandos probados:
  - `mvn -q clean package -DskipTests`: BUILD OK.
  - V31 aplicada en prod: `success=t` en `flyway_schema_history` (Flyway V31).
  - `python tools/smoke-test-vps.py`: 19/19 pantallas RENDER OK (incluida `articulos`).
- Cambios de datos: si, ver migracion V31 (siembra de catalogo + cambio de tipo de columna).
- Cambios de entorno: no.
- Impacto LLM/tokens: no.
- Decision esperada: aprobar (migracion sin datos previos en la columna).
- Notas para auditor:
  - La conversion usa `USING NULL` porque la columna no tenia datos cargados; verificar que no haya entorno con datos legacy en `articulo.clasificacion`.
  - La siembra corre despues de V28 (RLS): usa `SET LOCAL app.tenant=-1` via `set_config`.
  - El combo se llena solo con opciones activas (`catalogoService.opciones(...)`), lo que acota la seleccion a valores validos.

## Resumen Funcional

En el ABM de Articulos, el campo "Clasificacion" deja de ser texto libre y pasa a ser una lista
desplegable con opciones del catalogo (General, Servicio, Gasto, Otro), configurable por empresa en
"Listas del sistema".

## Resumen Tecnico

`Articulo.clasificacion` cambia de `String` a `Long` (id de `entidad`). `ArticuloBean` carga la lista
`CLASIFICACION_ARTICULO` con `catalogoService.opciones(...)` y `articulos.xhtml` la muestra con
`p:selectOneMenu` (itemValue = id). La migracion V31 siembra el catalogo global (tenant -1) y convierte
el tipo de la columna a `bigint`.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| dominio/catalogo/Articulo.java | `clasificacion` String -> Long |
| web/ArticuloBean.java | carga y expone `clasificaciones` |
| webapp/articulos.xhtml | `p:inputText` -> `p:selectOneMenu` |
| resources/db/migration/V31__clasificacion_articulo_lista.sql | NUEVO — siembra catalogo + ALTER COLUMN |

## Cambios De Datos

V31: INSERT idempotente de 4 opciones en `entidad` (lista='CLASIFICACION_ARTICULO', tenant=-1) +
`ALTER TABLE articulo ALTER COLUMN clasificacion TYPE bigint USING NULL`. Requiere
`set_config('app.tenant','-1',true)` por RLS (V28).

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Ver `test-plan.md`. Resumen: build OK, V31 aplicada en prod (`success=t`), smoke-test 19/19 RENDER OK.

## Pruebas Manuales Sugeridas

1. Entrar al ABM de Articulos -> pestana Clasificacion: el campo debe ser un combo con opciones del catalogo.
2. Guardar un articulo eligiendo una clasificacion y reabrirlo: debe conservar la seleccion.

## Riesgos Conocidos

- La conversion de tipo asume columna sin datos (USING NULL); un entorno con datos legacy perderia el valor de texto.
- Casos secundarios (cuenta contable, estado de plantillas) quedan fuera de alcance de este REQ.
