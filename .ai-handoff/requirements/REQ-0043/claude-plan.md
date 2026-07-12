# REQ-0043 - Plan De Implementacion

**Estado:** APROBADO_PARA_DESARROLLO
**Fecha:** 2026-07-12

## Estrategia

Aplicar el patron "campo libre -> lista de catalogo" ya usado en Estado civil y en
clasificacion de articulo (REQ-0031/0048): la columna guarda el id de `entidad`, el bean
carga las opciones activas y la vista usa `p:selectOneMenu`. Migracion de datos con backfill
para no perder lo cargado en texto libre.

## Archivos A Modificar

| Archivo | Cambio |
|---|---|
| dominio/persona/PersonaEmpresa.java | campo `nacionalidad` String -> Long (id de entidad) |
| web/PersonaBean.java | `nacionalidades = catalogoService.opciones("NACIONALIDADES")` + getter |
| webapp/personas.xhtml | `p:inputText` -> `p:selectOneMenu` con `#{personaBean.nacionalidades}` |
| resources/db/migration/V30__nacionalidad_lista.sql | siembra NACIONALIDADES (-1), convierte columna varchar->bigint con backfill, recrea vista v_persona |

## Pruebas Previstas

- [x] `mvn -q clean package -DskipTests` BUILD OK.
- [x] V30 validada en `BEGIN...ROLLBACK` contra la BD real y aplicada en prod (success=t).
- [x] smoke-test 19/19 RENDER OK incluida `personas`.

## Riesgos

Migracion de datos (varchar->bigint) con dependencia de la vista `v_persona`; se mitiga
dropeando/recreando la vista dentro de la misma migracion y fijando `app.tenant=-1` por RLS.

## Cambios De Datos

V30: alta de 14 opciones globales en `entidad` (lista NACIONALIDADES, tenant -1) y conversion
de `persona_empresa.nacionalidad` a bigint con backfill por descripcion.
