# REQ-0056 - Plan De Implementacion

**Estado:** APROBADO_PARA_DESARROLLO
**Fecha:** 2026-07-12

## Estrategia

Servicio @AislarTenant con KPIs (native queries, patron InicioService), montos por moneda (no mezcla).
Bean con filtros periodo/moneda. Vista con tarjetas + drill-down. Pantalla registrada por V37.

## Archivos A Modificar

| Archivo | Cambio |
|---|---|
| V37__pantalla_dashboard_gerencial.sql | registra pantalla |
| servicio/DashboardGerencialService.java | NUEVO — KPIs |
| web/DashboardGerencialBean.java | NUEVO |
| webapp/dashboard-gerencial.xhtml | NUEVO |
| WEB-INF/plantilla.xhtml + smoke | menu + cobertura |

## Pruebas Previstas

- [ ] Build OK
- [ ] V37 rollback + deploy + smoke
- [ ] Montos por moneda (no mezcla) + RLS por tenant

## Riesgos

- Bajo-medio: solo lectura. No mezclar monedas (mitigado por filtro moneda).

## Cambios De Datos

V37 registra pantalla `dashboard-gerencial`.
