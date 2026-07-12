# REQ-0043 - Analisis

**Estado:** EN_ANALISIS
**Fecha:** 2026-07-12
**Analista:** Claude

## Analisis Funcional

Hoy la Nacionalidad de una Persona es texto libre (`p:inputText`, varchar(80)), lo que
genera datos inconsistentes ("Paraguaya", "paraguayo", "PY", vacio...). Se pide que pase a
ser una lista seleccionable configurable desde la tabla `entidad` (Listas del sistema),
igual que Estado civil, Sexo o Actividad economica.

Decision de negocio resuelta con el usuario (ver `user-decision.md`):

- Nacionalidad es un **gentilicio** (paraguaya, argentina, brasilena), no un pais geografico.
- Se administra desde `entidad` con la lista `NACIONALIDADES`.
- No se reutiliza la geografia de Pais (residencia/origen es otra informacion).
- Debe poder agregarse una nueva nacionalidad sin tocar codigo.

El combo debe ofrecer solo opciones activas, respetar el estandar global (-1) + tenant, y no
perder los datos ya cargados en texto libre.

## Analisis Tecnico

- `PersonaEmpresa.nacionalidad` cambia de `String` a `Long` (guarda el id de la fila de
  `entidad`, mismo patron que las demas referencias a catalogo post-V26).
- `PersonaBean` expone `nacionalidades = catalogoService.opciones("NACIONALIDADES")` con su
  getter, cargado en el mismo bloque que roles/estadosCiviles/actividades.
- `personas.xhtml`: el `p:inputText` de Nacionalidad se reemplaza por `p:selectOneMenu` con
  `filter="true"`, opcion "Sin especificar" (null) y `f:selectItems` sobre
  `#{personaBean.nacionalidades}` (itemLabel descripcion, itemValue id).
- Migracion Flyway `V30__nacionalidad_lista.sql`, que corre DESPUES de V28 (RLS): fija
  `app.tenant=-1` con `set_config`, siembra 14 gentilicios globales, y convierte la columna
  varchar->bigint con backfill por descripcion (case-insensitive). La vista `v_persona`
  depende de la columna, asi que se dropea y se recrea identica a V26 con la columna ya bigint.

## Riesgos

| Riesgo | Probabilidad | Impacto | Mitigacion |
|---|---|---|---|
| Perder datos de nacionalidad en texto libre | media | medio | backfill por descripcion; lo no matcheado queda NULL (re-seleccionable), no se borra la fila |
| La vista v_persona bloquea el ALTER de la columna | alta | medio | DROP VIEW antes del ALTER y recrear identica a V26 dentro de la misma migracion |
| Insertar/actualizar con RLS activa (V28) falla | media | alto | `set_config('app.tenant','-1',true)` al inicio de la migracion |
| Textos libres raros ("PY") no matchean | media | bajo | quedan NULL; el usuario re-selecciona del combo (dato de bajo volumen) |

**Semaforo Codex:** medio

## Preguntas Abiertas

- [ ] Ninguna (decision de gentilicio vs Pais resuelta por el usuario).

## Impacto En Costos / LLM

- Aumenta tokens por mensaje: no
- Agrega llamadas extra al LLM: no
- Puede resolverse sin IA: si

## Impacto En Datos

- Requiere migracion: si (V30: siembra NACIONALIDADES + convierte columna + recrea vista)
- Tablas/colecciones afectadas: `entidad` (alta de 14 opciones globales), `persona_empresa`
  (columna `nacionalidad` varchar->bigint), vista `v_persona` (drop/recreate identica).

## Recomendacion

**Desarrollar** — riesgo medio (migracion de datos con backfill), alto valor de normalizacion;
patron ya probado en Estado civil / Actividad economica y en clasificacion de articulo
(REQ-0031/0048).
