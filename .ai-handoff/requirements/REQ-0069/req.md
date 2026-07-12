# REQ-0069 - Motor de metricas gerenciales comparativas para dashboard

**Numero:** REQ-0069
**Fecha de creacion:** 2026-07-12
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

Del usuario (2026-07-12): "Analiza el dashboard, me parece muy simple. Deberia de tener comparativos entre año, mes contra mes, mes del mismo año contra el mes de hace 1 año."

## Objetivo Funcional

Construir una capa de metricas gerenciales reutilizable para que el dashboard no muestre solo valores
puntuales, sino comparativos consistentes: periodo actual contra periodo anterior, mes actual contra
mismo mes del año anterior, y acumulado anual contra acumulado del año anterior.

## Criterios De Aceptacion

- [x] Existe `DashboardGerencialService` ampliado o servicio nuevo de metricas que devuelva DTOs tipados para KPI actual, periodo anterior, mismo periodo año anterior, acumulado anual y variaciones. (nuevo `DashboardMetricasService`; DTO `Comparativo` con actual/periodoAnterior/mismoMesAnioAnterior/ytd/ytdAnterior + Variacion mom/yoy/ytdVar)
- [x] Se calculan variaciones absolutas y porcentuales con direccion semantica: mejora, empeora, neutro. (`Variacion.entre`: absoluta, porcentual, Direccion MEJORA/EMPEORA/NEUTRO/NA segun MAS_ES_MEJOR por indicador)
- [x] Los periodos soportados incluyen: mes actual, mes anterior, mismo mes del año anterior, año a la fecha y año anterior a la fecha. (`Periodos.para`: actual/anterior/mismoMesAnioAnterior/ytd/ytdAnterior)
- [x] Los indicadores minimos comparativos son: cobros, mora, ingresos, egresos, rentabilidad, ocupacion, vacancia, contratos nuevos y contratos finalizados. (9 indicadores implementados)
- [x] Ningun indicador monetario mezcla monedas; si no hay moneda unica, exige filtro de moneda. (cobros/mora se filtran por :mon; sin moneda el KPI monetario queda aplicable=false; ingreso_egreso es moneda base de la empresa)
- [x] Los calculos usan `BigDecimal` para montos y porcentajes con escala definida. (BigDecimal; porcentajes escala 2 HALF_UP)
- [x] El servicio respeta tenant/RLS y permisos de lectura del dashboard. (@AislarTenant + RLS V28; exigir dashboard-gerencial/VER; contexto global -> vacio)
- [x] Las consultas tienen limites, filtros e indices razonables; no deben bloquear la operacion diaria. (solo agregados COUNT/SUM acotados por rango de fecha; usan indices de fecha/estado existentes; sin N+1)
- [x] Cada KPI devuelve metadata de evidencia: nombre del indicador, periodo, filtros aplicados y clave de drill-down para REQ-0074. (Comparativo: indicador/etiqueta/periodoDesc/monedaId/sucursalId/drillKey)

## Reglas De Negocio

- La comparacion mes contra mes compara periodos completos equivalentes cuando se elige mes cerrado, y dias transcurridos equivalentes cuando el periodo actual esta en curso.
- La comparacion con el año anterior debe usar el mismo rango calendario.
- Si el periodo no tiene base comparable, la variacion porcentual debe mostrarse como no aplicable, no como infinito ni cero falso.

## Dependencias

- Depende de: REQ-0056, REQ-0062.
- Requerido por: REQ-0070, REQ-0071, REQ-0072, REQ-0073, REQ-0074, REQ-0075.

## Fuentes Y Trazabilidad

- Pedido directo del usuario sobre comparativos año/mes.
- Dashboard actual: `DashboardGerencialService` y `dashboard-gerencial.xhtml`.
