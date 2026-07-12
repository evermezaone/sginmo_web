# REQ-0071 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0071
- Tipo de cambio: BD (V49: pantalla) + backend (RentabilidadService) + UI (rentabilidad.xhtml/Bean)
- Riesgo: bajo (solo lectura)
- Archivos clave:
  - `V49__pantalla_rentabilidad.sql`: pantalla `rentabilidad`.
  - `servicio/RentabilidadService.java` (@AislarTenant): resumen(desde,hasta) -> ingresos/egresos por tipo (GROUP BY articulo.aplicacion), totalIngresos operativos, totalEgresos, totalDepositos (pasivo), neto, margenPct; rankingActivos por neto. Base CAJA desde `ingreso_egreso` estado CANCELADO (moneda base). DEPOSITO_GARANTIA/GARANTIA excluidos del neto (pasivo).
  - `web/RentabilidadBean.java` + `webapp/rentabilidad.xhtml`: KPIs (ingresos/egresos/neto/margen/depositos) + ingresos por tipo + egresos por tipo + ranking de activos, con filtro de periodo.
  - `WEB-INF/plantilla.xhtml`, `tools/smoke-test-vps.py`: menu + cobertura.
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - Deploy + Flyway V49; `python tools/smoke-test-vps.py`: 33/33 incl. `rentabilidad`.
- Cambios de datos: si, V49 (pantalla). Cambios de entorno: no.
- Decision esperada: aprobar; revisar la eleccion de fuente (ingreso_egreso base caja) y la exclusion del deposito.
- Notas para auditor:
  - No mezcla monedas: `ingreso_egreso` no tiene columna moneda -> es de la moneda base de la empresa (base caja monomoneda). Cobros multi-moneda se ven en el dashboard (0069/0070).
  - Clasificacion por `articulo.aplicacion` (dato); el mapeo a etiqueta es solo presentacion.
  - Deposito/garantia = pasivo de terceros, no rentabilidad (RN explicita).
  - RLS/tenant: @AislarTenant; contexto global -> vacio. drillKey listo para 0074.
  - Base devengada queda como extension futura (documentado en el REQ).

## Resumen Funcional

Pantalla "Rentabilidad": ingresos y egresos del periodo por tipo, resultado neto, margen %, deposito de
garantia separado (no rentabilidad) y ranking de activos por rentabilidad neta.

## Resumen Tecnico

RentabilidadService @AislarTenant de solo lectura; agrega ingreso_egreso por articulo.aplicacion.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| V49__pantalla_rentabilidad.sql | NUEVO - pantalla |
| servicio/RentabilidadService.java | NUEVO |
| web/RentabilidadBean.java + webapp/rentabilidad.xhtml | NUEVOS |
| WEB-INF/plantilla.xhtml, tools/smoke-test-vps.py | menu + cobertura |

## Cambios De Datos

V49: pantalla rentabilidad.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; Flyway V49; smoke 33/33.

## Pruebas Manuales Sugeridas

1. Con ingreso_egreso real: verificar ingresos/egresos por tipo, neto y margen.
2. Confirmar que el deposito/garantia no entra al neto.
3. Ranking de activos por neto (mejores arriba, peores al final).

## Limitaciones Conocidas

- Base caja monomoneda (ingreso_egreso). Base devengada y multi-moneda: extension futura.
- Breakdown por tipo de activo/zona/propietario: incremental (hoy ranking por activo).

## Riesgos Conocidos

- Solo lectura; riesgo bajo.
