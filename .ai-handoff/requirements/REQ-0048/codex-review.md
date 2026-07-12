# REQ-0048 - Codex Review

## Decision

APROBADO_POR_CODEX.

## Revision

- `Articulo.clasificacion` pasa a id de `entidad`.
- `ArticuloBean` carga `CLASIFICACION_ARTICULO` con el patron de catalogos.
- `articulos.xhtml` reemplaza texto libre por combo.
- `V31__clasificacion_articulo_lista.sql` siembra opciones globales y cambia el tipo de columna.

## Nota De Riesgo

La migracion usa `ALTER COLUMN ... USING NULL`; esto es aceptable solo bajo la evidencia de entrega de que no habia datos productivos previos en `articulo.clasificacion`. Si luego aparece una base legacy con valores en esa columna, debe hacerse migracion de backfill por descripcion antes de aplicar V31.

## Verificacion

- `mvn -q clean package`: OK.
