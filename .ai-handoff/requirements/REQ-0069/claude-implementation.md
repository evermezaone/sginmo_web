# REQ-0069 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0069
- Tipo de cambio: backend (servicio de metricas, solo lectura). Sin BD, sin UI (la UI la aporta REQ-0070).
- Riesgo: bajo (solo SELECTs agregados; no muta datos).
- Archivos clave:
  - `servicio/DashboardMetricasService.java`: motor comparativo. `comparativos(mesRef, moneda, sucursal)` -> List<Comparativo>. Periodos.para() calcula actual/anterior/mismoMesAnioAnterior/ytd/ytdAnterior (mes en curso -> mismos dias transcurridos; mes cerrado -> mes completo; YTD mismo rango calendario ano vs ano). Variacion.entre() da absoluta, porcentual (escala 2) y Direccion (MEJORA/EMPEORA/NEUTRO/NA; NA si no hay base comparable -no infinito ni cero falso-). 9 indicadores: cobros, mora, ingresos, egresos, rentabilidad, ocupacion, vacancia, contratos_nuevos, contratos_finalizados.
- Comandos probados:
  - `mvn -q clean package` (multi-modulo): BUILD OK (EXIT 0).
  - Deploy + redeploy: login HTTP 200; `python tools/smoke-test-vps.py`: 31/31 (sin regresion; es backend).
- Cambios de datos: no. Cambios de entorno: no. Impacto LLM/tokens: no.
- Decision esperada: aprobar; revisar semantica de periodos y de la variacion NA.
- Notas para auditor:
  - No mezcla monedas: cobros/mora se filtran por :mon; sin moneda el KPI monetario queda `aplicable=false`. `ingreso_egreso` es de la moneda base (no tiene columna moneda) -> ingresos/egresos/rentabilidad en base.
  - RLS: @AislarTenant + `exigir(dashboard-gerencial, VER)`; contexto global (-1) devuelve lista vacia.
  - Consultas: solo COUNT/SUM acotados por rango de fecha; ocupacion/mora son snapshot a la fecha de corte.
  - Ocupacion/vacancia usan el universo "alquilable" (precio_alquiler>0 y estado<>VENDIDA); se refina en REQ-0072.

## Resumen Funcional

El dashboard podra mostrar cada KPI con su comparativo (mes vs mes anterior, mes vs mismo mes del ano
pasado, YTD vs YTD anterior) y una direccion de tendencia consistente.

## Resumen Tecnico

Servicio @AislarTenant de solo lectura; DTO Comparativo + Variacion; calculo de periodos centralizado.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| servicio/DashboardMetricasService.java | NUEVO - motor de metricas comparativas |

## Cambios De Datos

Sin cambios.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; deploy; smoke 31/31 (sin regresion). El servicio se ejercita visualmente en REQ-0070.

## Pruebas Manuales Sugeridas

1. Consumir `comparativos()` desde el dashboard (REQ-0070) y validar mom/yoy/ytd con datos reales.
2. Mes en curso: verificar que el comparativo usa los mismos dias transcurridos.

## Limitaciones Conocidas

- Sin superficie visual propia (por diseno: REQ-0070 la aporta). Verificacion funcional plena llega con 0070.
- Ocupacion historica aproxima con rangos de fecha de la operacion; el universo alquilable se refina en REQ-0072.

## Riesgos Conocidos

- Solo lectura; riesgo bajo.
