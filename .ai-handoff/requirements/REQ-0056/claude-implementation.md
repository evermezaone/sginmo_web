# REQ-0056 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0056
- Tipo de cambio: backend + UI + BD (solo registra pantalla)
- Riesgo: bajo-medio (solo lectura; KPIs; sin cambios de esquema de negocio)
- Archivos clave:
  - `V37__pantalla_dashboard_gerencial.sql`: registra la pantalla `dashboard-gerencial`.
  - `servicio/DashboardGerencialService.java`: @AislarTenant; KPIs con filtros periodo/moneda/sucursal; montos por moneda (no mezcla).
  - `web/DashboardGerencialBean.java`: filtros + moneda default (guarani) + recalcular.
  - `webapp/dashboard-gerencial.xhtml`: filtros + tarjetas KPI con drill-down (h:link).
  - `WEB-INF/plantilla.xhtml`, `tools/smoke-test-vps.py`: menu + cobertura.
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - V37 en `BEGIN...ROLLBACK`: pantalla registrada.
  - Deploy + Flyway V37 `success=t`; `python tools/smoke-test-vps.py`: 23/23 RENDER OK incl. `dashboard-gerencial`.
- Cambios de datos: si, V37 (solo alta de la pantalla en `entidad`).
- Cambios de entorno: no.
- Impacto LLM/tokens: no.
- Decision esperada: aprobar (riesgo bajo-medio, solo lectura).
- Notas para auditor:
  - "No mezclar monedas": los KPIs monetarios (monto vencido, cobrado) filtran por la moneda seleccionada (cronograma_cuota.moneda / cobro.moneda). Los conteos no aplican moneda.
  - Todos los indicadores son datos reales (sin proyecciones); etiquetado en la vista.
  - @AislarTenant fija app.tenant -> RLS filtra por empresa; en contexto global (-1) devuelve cero.

## Resumen Funcional

Nuevo "Dashboard gerencial": 7 KPIs (operaciones activas, ventas, alquileres, propiedades disponibles,
cuotas vencidas, monto vencido, cobrado en el periodo) con filtros de periodo y moneda; cada tarjeta
enlaza al modulo que la explica.

## Resumen Tecnico

DashboardGerencialService @AislarTenant calcula los KPIs con native queries (patron InicioService),
montos en BigDecimal por moneda. Bean @ViewScoped con periodo (mes actual por defecto) y moneda (guarani
por defecto). Vista con tarjetas + drill-down.

## Limitaciones Conocidas (transparencia)

- Filtro de sucursal: soportado en el servicio (parametro), combo UI diferido (refinamiento).
- Drill-down: enlaza al modulo (operaciones/caja/activos); pre-aplicar el filtro exacto en el destino es refinamiento.
- Mora avanzada (dias de mora, gestion): es REQ-0057.

## Archivos Modificados

Ver Manifiesto. V37 nueva.

## Cambios De Datos

V37: registra pantalla `dashboard-gerencial` en `entidad` (tenant -1). Sin cambios de datos de negocio.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; V37 rollback OK; deploy + Flyway success; smoke 23/23. Ver test-plan.

## Pruebas Manuales Sugeridas

1. Abrir Dashboard gerencial con una empresa con operaciones/cobros: verificar KPIs y cambio de periodo/moneda.

## Riesgos Conocidos

- Solo lectura; sin cambios de esquema de negocio. Ver "Limitaciones".
