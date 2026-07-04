# Estandares De Desarrollo - SGInmo Web (SGI)

Estandares para la migracion del ERP inmobiliario SGInmo (C# WinForms + EF5 + Firebird + Crystal) a WildFly 40 + JSF/PrimeFaces + JPA/Hibernate + PostgreSQL + JasperReports.

Objetivo: que cada REQ se implemente con criterios consistentes, verificables y alineados con la logica heredada documentada en `docs-migracion/` (00-07).

## Uso obligatorio

- Claude debe leer esta carpeta antes de implementar REQs nuevos o corregir REQs existentes.
- Codex debe usar estos documentos como checklist de auditoria.
- Si un REQ necesita apartarse de un estandar, debe justificarlo en `analysis.md` y `claude-implementation.md`.
- Si una regla de negocio proviene del legado, debe citar el ID `RN-*`, el archivo C# (`FrmX.cs`/`XService.cs`), el SP Firebird (`RPT_*`) o el valor de `DOMINIOS` usado como fuente (ver `source-traceability.md`).

## Documentos

- `backend-jakarta.md`: CDI, `@Transactional`, JPA, BigDecimal, enums, seguridad backend, auditoria.
- `frontend-jsf-primefaces.md`: beans, LazyDataModel, dialogs, patrones de UI heredados.
- `database-postgresql.md`: naming, Flyway, constraints, ETL desde Firebird.
- `source-traceability.md`: como trazar reglas a una fuente de verdad; regla anti-invencion.
- `audit-checklist.md`: checklist minimo antes de enviar a Codex.
- `audit-categories.md`: vocabulario controlado de observaciones.
- `workflow-priority.md`: regla del menor REQ pendiente y aprendizaje acumulado.

## Principios base

1. Backend manda en reglas de negocio; la UI complementa, no reemplaza.
2. Toda operacion multi-tabla es transaccional (`@Transactional`) — regla critica del proyecto.
3. Los nombres de tablas/columnas se cotejan contra el esquema real antes de escribir SQL/JPQL.
4. Los permisos protegen la capa de servicio (`@RolesAllowed`), no solo esconden botones.
5. Todo cambio que toque datos, montos, estados, numeracion, cobros o cuotas requiere revision estricta.
6. Las reglas criticas se trazan a fuente de verdad: doc RN-*, codigo legado, SP Firebird, DOMINIOS o decision del usuario.
7. Cerrar primero el menor REQ pendiente; no saltar salvo excepcion explicita.
8. Todo hallazgo repetible se transforma en estandar y se aplica a los REQs pendientes.
9. Los 7 bugs del legado (CLAUDE.md) se corrigen siempre; no se replican.
