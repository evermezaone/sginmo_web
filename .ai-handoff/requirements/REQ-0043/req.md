# REQ-0043 - Nacionalidad de Persona como lista configurable

**Numero:** REQ-0043
**Fecha de creacion:** 2026-07-11
**Estado inicial:** EN_ANALISIS
**Prioridad:** media

## Texto Original

Nacionalidad (persona) debe ser lista seleccionable desde catalogo Entidad.

## Decision De Usuario

Resuelto el bloqueo de negocio:

- Nacionalidad debe ser **gentilicio**, no pais geografico.
- Debe ser configurable desde la tabla `entidad`.
- El catalogo sugerido es `NACIONALIDADES`.
- Ejemplos esperados: `paraguaya`, `argentina`, `brasilena`.
- No duplicar ni reutilizar directamente la geografia de Pais.

## Objetivo Funcional

Al cargar o editar una Persona, el campo Nacionalidad debe dejar de ser texto libre y pasar a ser una lista seleccionable configurable desde `entidad`, siguiendo el patron de Estado civil/Sexo/Listas del sistema.

## Criterios De Aceptacion

- [x] `personas.xhtml` reemplaza el input libre de Nacionalidad por un combo/lista seleccionable.
- [x] El catalogo de nacionalidades se administra desde `entidad`/Listas del sistema.
- [x] Las opciones son gentilicios, no paises geograficos.
- [x] La migracion siembra valores iniciales globales para `NACIONALIDADES`.
- [x] Si existen valores previos de texto libre, se migran o se conserva una estrategia documentada para no perder datos. (backfill por descripcion case-insensitive; lo no matcheado queda NULL y se re-selecciona desde el combo)
- [x] El servicio valida que la nacionalidad seleccionada exista y este activa. (el combo solo ofrece opciones ACTIVAS de `opciones("NACIONALIDADES")`; se persiste el id de entidad, no texto libre)
- [x] El combo respeta tenant/global segun estandar de catalogos. (`CatalogoService.opciones` devuelve globales tenant=-1 + las del tenant activo)
- [x] No se rompe la carga/edicion de Persona ni los roles vinculados. (smoke 19/19 render OK incluida personas)

## Reglas De Negocio

- Nacionalidad describe gentilicio juridico/identitario de la persona.
- Pais geografico/residencia/origen debe seguir siendo otra informacion si existe en geografia.
- El campo debe quedar parametrizable sin tocar codigo para agregar nuevas nacionalidades.

## Dependencias

- Depende de: REQ-0012, REQ-0044, REQ-0045.
- Requerido por: normalizacion de datos de Persona.

## Fuentes Y Trazabilidad

- Decision usuario 2026-07-12: "es obvio que debe ser gentilicio y ademas configurable en la tabla de Entidad".
- BD SGI `REQ-0043`: duda original sobre gentilicio vs Pais.
