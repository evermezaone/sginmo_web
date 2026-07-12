# REQ-0043 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package -DskipTests` | Build OK | OK |
| T02 | V30 en `BEGIN...ROLLBACK` contra BD real | siembra 14 NACIONALIDADES, columna nacionalidad pasa a bigint, backfill OK, vista v_persona recreada, rollback limpio | OK |
| T03 | Deploy + Flyway V30 en prod | success=t | OK |
| T04 | `python tools/smoke-test-vps.py` | 19/19 RENDER OK incl. `personas` | OK (RESULTADO: TODAS OK) |
| T05 | Render de `personas` ejercita el combo Nacionalidad | la pagina responde 200 y el combo carga las opciones activas | OK |

## Pruebas Manuales

| ID | Escenario | Pasos | Resultado esperado | Resultado real |
|---|---|---|---|---|
| M01 | Ver combo | Login -> Personas -> editar -> pestana Datos -> Nacionalidad | combo con gentilicios y filtro por texto | pendiente (verificacion visual del usuario) |
| M02 | Persistencia | Seleccionar nacionalidad, guardar, reabrir | la seleccion se conserva (guarda el id de entidad) | pendiente (verificacion del usuario) |

## Revision Transversal

- Patron aplicado: campo libre -> lista de catalogo. Es el MISMO patron de REQ-0031/0048
  (clasificacion de articulo): la columna guarda el id de `entidad`, el bean carga
  `catalogoService.opciones(...)` (ACTIVO + tenant IN(-1,:t)) y la vista usa `p:selectOneMenu`
  con itemValue=id. Busqueda: `grep opciones(` en `web/` -> roles/estadosCiviles/actividades/
  clasificacionArticulo/nacionalidades usan la misma via.
- Siembra de catalogo con `set_config('app.tenant','-1',true)`: identico a V30/V31/V32 (patron
  post-RLS). Busqueda: `grep set_config db/migration`.
- Dependencia de vista: `v_persona` se recrea identica a V26 (misma proyeccion de columnas);
  no se altero ninguna regla de negocio compartida (dinero/estados/cobros).

## Nota De Alcance

El backfill preserva los datos que matchean por descripcion; los valores libres raros quedan
NULL y se re-seleccionan del combo (dato de bajo volumen, sin regla de negocio dependiente).

## Datos De Prueba

Las 14 opciones sembradas por V30 (PARAGUAYA..OTRA) como catalogo global (tenant -1).
