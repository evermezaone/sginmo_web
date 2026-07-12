# REQ-0069 - Auditoria Codex

Fecha: 2026-07-12T16:28:03-03:00
Auditor: codex
Decision: APROBADO EN RONDA 2

## Alcance revisado

- `.ai-handoff/requirements/REQ-0069/req.md`
- `.ai-handoff/requirements/REQ-0069/claude-implementation.md`
- `.ai-handoff/requirements/REQ-0069/preaudit-checklist.md`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/DashboardMetricasService.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/DashboardGerencialBean.java`
- `Desarrollo/sginmo-web/src/main/resources/db/migration/V1__esquema_inicial.sql`

## Verificacion

- `mvn -q clean package` en `Desarrollo`: OK.
- Inspeccion de codigo real: completada.

## Hallazgos

### Obs 1 - Metodos publicos del motor saltan el permiso del dashboard

Problema: `DashboardMetricasService.comparativos()` y `serieMensual()` llaman `autorizacion.exigir("dashboard-gerencial","VER")`, pero los otros metodos publicos reutilizables no lo hacen: `comparativo(String, Periodos, Long, Long)` y `valorMesActual(String, Long, Long)`. El propio comentario los declara reutilizados por REQ-0070/0074/0075 y objetivos. El criterio del REQ exige que el servicio respete permisos de lectura del dashboard, no solo un endpoint puntual.

Impacto: cualquier bean/servicio que inyecte `DashboardMetricasService` puede obtener metricas del dashboard sin pasar por el permiso `dashboard-gerencial:VER`, dependiendo solo de RLS. Esto debilita el enforcement backend y contradice el estandar de no confiar en checks de UI.

Solucion esperada: centralizar el check de permiso en todos los metodos publicos de lectura del motor, o hacer privados/package-private los metodos que no deban ser API autorizable. Si `valorMesActual()` se usa desde objetivos con otro permiso legitimo, separar una API interna clara y documentada, no dejar el bypass como metodo publico general.

Evidencia:
- `DashboardMetricasService.java:80-94`
- `DashboardMetricasService.java:121-144`

### Obs 2 - El filtro de sucursal queda registrado pero no se aplica consistentemente a ocupacion/vacancia

Problema: el DTO devuelve `sucursalId` para todos los comparativos, pero `alquilables(Long sucursal)` ignora el parametro y cuenta todos los activos alquilables del tenant. En cambio `ocupados()` si filtra por `o.sucursal`. Como `activo` no tiene columna sucursal y `operacion` si, el KPI con sucursal calcula un denominador de empresa completa y un numerador de la sucursal, pero la metadata dice que el filtro sucursal fue aplicado.

Impacto: ocupacion y vacancia por sucursal pueden quedar matematicamente falsas. Ejemplo: si una sucursal tiene 10 ocupados y la empresa 220 activos alquilables, se mostraria 4,55% aunque el universo operativo de esa sucursal sea otro. REQ-0069 exige metadata de filtros aplicados y comparativos consistentes; este filtro engaña al dashboard y al futuro drill-down de REQ-0074.

Solucion esperada: para indicadores cuyo universo no puede filtrarse por sucursal, no aceptar/propagar `sucursalId` en la metadata y mostrar el dato como empresa completa; o definir una regla trazable para asignar activos a sucursal y aplicar el mismo universo a `alquilables`, `ocupados` y `vacantes`. La evidencia/drill-down debe usar la misma regla que el KPI.

Evidencia:
- `DashboardMetricasService.java:200-231`
- `V1__esquema_inicial.sql:290-306` (`activo` sin sucursal)
- `V1__esquema_inicial.sql:354-372` (`operacion.sucursal`)

## Resultado

No apruebo REQ-0069. El motor esta bien encaminado y compila, pero debe corregir el enforcement de permisos en la API publica y la semantica del filtro sucursal en ocupacion/vacancia antes de destrabar REQ-0070..REQ-0075.

## Re-auditoria Ronda 2 - 2026-07-12

Decision: APROBADO_POR_CODEX

Verificacion:

- `mvn -q clean package` en `Desarrollo`: OK.
- Relectura de `DashboardMetricasService.java`: completada.
- Revision de consumidores de `valorMesActual`: `ObjetivoService` exige `objetivos:VER` antes de calcular/listar objetivos; `AlertaService` consume objetivos mediante esa API autorizada.

Observaciones cerradas:

- Obs 1: corregida. `comparativo(...)` ahora es privado y `valorMesActual(...)` ya no es publico; queda package-private y documentado como API interna para servicios gerenciales del mismo paquete con permisos propios. Las entradas publicas del dashboard (`comparativos`, `serieMensual`) siguen exigiendo `dashboard-gerencial:VER`.
- Obs 2: corregida. Ocupacion/vacancia ya no filtran solo una parte por sucursal; `valor()` llama a `ocupacionPct(r.hasta)` y `vacantes(r.hasta)`, y `alquilables/ocupados` calculan el universo de tenant de forma consistente. El comentario explicita que no se aplica sucursal porque `activo` no tiene esa columna.

Resultado final:

REQ-0069 aprobado. Puede destrabarse la auditoria de REQ-0070..REQ-0075 por prioridad.
