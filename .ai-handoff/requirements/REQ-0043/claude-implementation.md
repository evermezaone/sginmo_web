# REQ-0043 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0043
- Tipo de cambio: UI + dominio + BD (migracion de datos)
- Riesgo: medio (migracion varchar->bigint con backfill y recreacion de vista)
- Archivos clave:
  - `sginmo-web/src/main/java/py/com/pysistemas/sginmo/dominio/persona/PersonaEmpresa.java`: campo `nacionalidad` de `String` a `Long` (id de entidad, lista NACIONALIDADES).
  - `sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/PersonaBean.java`: `nacionalidades = catalogoService.opciones("NACIONALIDADES")` en la carga + `getNacionalidades()`.
  - `sginmo-web/src/main/webapp/personas.xhtml`: `p:inputText` de Nacionalidad -> `p:selectOneMenu` (filter, "Sin especificar"=null, `f:selectItems` sobre `#{personaBean.nacionalidades}` itemValue=id).
  - `sginmo-web/src/main/resources/db/migration/V30__nacionalidad_lista.sql`: `set_config(app.tenant,-1)`, siembra 14 gentilicios globales, DROP/recrea `v_persona`, convierte `persona_empresa.nacionalidad` varchar->bigint con backfill por descripcion.
- Comandos probados:
  - `mvn -q clean package -DskipTests`: BUILD OK.
  - V30 en `BEGIN...ROLLBACK` contra la BD real: siembra + conversion + vista OK, rollback limpio.
  - Deploy a la VPS + Flyway V30 `success=t` en prod.
  - `python tools/smoke-test-vps.py`: 19/19 pantallas RENDER OK, incluida `personas`.
- Cambios de datos: si, ver V30 (alta de 14 opciones globales; columna nacionalidad varchar->bigint con backfill).
- Cambios de entorno: no.
- Impacto LLM/tokens: no.
- Decision esperada: aprobar (riesgo medio, patron ya probado en Estado civil / clasificacion articulo).
- Notas para auditor:
  - El backfill mapea por `lower(trim(descripcion))`; lo no matcheado queda NULL (dato libre no catalogado, re-seleccionable), no se borra la fila.
  - La vista `v_persona` se recrea IDENTICA a V26 (misma lista de columnas, ahora `pe.nacionalidad` es bigint).
  - La columna `persona_empresa.clasificacion_fiscal` NO se toca aca (la baja de UI es REQ-0044; la columna queda deprecada, no dropeada).

## Resumen Funcional

Al cargar o editar una Persona, la Nacionalidad deja de ser texto libre y pasa a ser una
lista seleccionable de gentilicios (paraguaya, argentina, brasilena...) configurable desde
`entidad` (lista NACIONALIDADES). El combo ofrece solo opciones activas visibles al tenant
(-1 global + propio), con opcion "Sin especificar". Los datos previos en texto libre se
conservan por backfill segun su descripcion.

## Resumen Tecnico

`PersonaEmpresa.nacionalidad` pasa a `Long` = id de `entidad`. `PersonaBean` carga
`opciones("NACIONALIDADES")` (JPQL: estado ACTIVO y tenant IN(-1, actual), orden por
descripcion) y la expone con getter. `personas.xhtml` usa `p:selectOneMenu` con `filter` e
itemValue = id. La migracion `V30` corre despues de V28 (RLS): `set_config(app.tenant,-1)`,
INSERT idempotente de 14 gentilicios en `entidad` (tenant -1), DROP de `v_persona`, ADD de
columna `nacionalidad_id` bigint, UPDATE de backfill por descripcion, DROP de la columna
varchar, RENAME a `nacionalidad`, y CREATE VIEW `v_persona` identica a V26.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| dominio/persona/PersonaEmpresa.java | campo `nacionalidad` String -> Long |
| web/PersonaBean.java | carga `nacionalidades` + getter `getNacionalidades()` |
| webapp/personas.xhtml | `p:inputText` -> `p:selectOneMenu` de Nacionalidad |
| resources/db/migration/V30__nacionalidad_lista.sql | NUEVO — siembra + conversion columna + recrea vista |

## Cambios De Datos

V30: INSERT idempotente de 14 filas en `entidad` (lista='NACIONALIDADES', tenant=-1, estado
ACTIVO). Conversion de `persona_empresa.nacionalidad` de varchar(80) a bigint con backfill por
descripcion (case-insensitive); lo no matcheado queda NULL. DROP/recreate de la vista
`v_persona` (identica a V26). Requiere `set_config('app.tenant','-1',true)` por RLS (V28).

## Variables De Entorno

Ninguna nueva.

## Pruebas Ejecutadas

Ver `test-plan.md`. Resumen: build OK, V30 validada en rollback y aplicada en prod
(success=t), smoke-test 19/19 RENDER OK incluida `personas`.

## Pruebas Manuales Sugeridas

1. Editar una Persona -> pestana Datos -> Nacionalidad ahora es combo con gentilicios y filtro.
2. Guardar con una nacionalidad seleccionada y reabrir: la seleccion persiste (guarda el id).

## Riesgos Conocidos

- Textos libres previos que no matchean por descripcion quedan NULL (se re-seleccionan del combo).
- La columna `clasificacion_fiscal` sigue existiendo en BD (deprecada por REQ-0044, no dropeada).
