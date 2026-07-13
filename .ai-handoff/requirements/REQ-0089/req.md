# REQ-0089 - BUG Personas: los cambios de Roles no se guardan al editar

**Numero:** REQ-0089
**Fecha de creacion:** 2026-07-13
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

Bug: al editar una persona existente, el usuario marca/desmarca/cambia roles, pero al guardar el sistema no persiste (los roles anteriores se mantienen o el cambio se pierde). Esperado: al guardar la persona, actualizar la lista de roles en la BD, eliminando los desmarcados e insertando los nuevos.

## Causa Raiz

El ABM tenia un modelo mixto: `agregarRol`/`quitarRol` persistian INMEDIATAMENTE por accion, pero el boton "Guardar" (`guardarRolesPendientes`) SOLO insertaba los roles pendientes (id nulo) y NUNCA daba de baja los desmarcados. No habia reconciliacion al guardar, por lo que los cambios de rol en la edicion no quedaban consistentes.

## Solucion

Modelo diferido + reconciliacion al guardar (coincide con el comportamiento esperado):
- `agregarRol`/`quitarRol` solo modifican la lista en memoria (no tocan la BD).
- Al `guardar()`, `PersonaService.reconciliarRoles(personaId, rolesDeseados)` inserta/reactiva los roles que faltan y da de baja logica (INACTIVO) los activos que ya no estan, respetando pertenencia/tenant y preservando historial.

## Criterios De Aceptacion

- [x] Al editar una persona y agregar un rol, al Guardar el rol queda ACTIVO en la BD.
- [x] Al editar y quitar un rol, al Guardar el rol queda INACTIVO (baja logica, preserva historial).
- [x] Combinar altas y bajas en una misma edicion se persiste correctamente al Guardar.
- [x] Respeta el aislamiento por tenant (no toca roles de otra empresa) y la pertenencia a la cartera.
- [x] Alta de persona nueva con roles sigue funcionando (se insertan al guardar).
- [x] Build `mvn -q clean package` EXIT 0; smoke 36/36 (personas 200).

## Dependencias

- Depende de: REQ-0012 (personas/roles), REQ-0036 (aislamiento por tenant).
