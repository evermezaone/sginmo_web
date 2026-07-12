# REQ-0052 - Plan De Implementacion

**Estado:** APROBADO_PARA_DESARROLLO
**Fecha:** 2026-07-12

## Estrategia

Tabla nueva de negocio `agenda_evento` (100% por-tenant, RLS inline igual que
`documento_generado` de V29), con eventos manuales (tareas) y automaticos (vencimientos).
Generacion automatica ON-DEMAND al abrir la agenda/tablero (no hay scheduler EJB hoy;
se documenta @Schedule como mejora futura), con dedup por indice unico para no duplicar
al reabrir. LazyDataModel para el listado con filtros. Se reutiliza `app_tenant()`,
`@AislarTenant`/`TenantInterceptor`, `CorreoService.enviarAsync` (opcional, no en este REQ),
y `operacion.fecha_fin_contrato`/`cronograma_cuota` como fuentes de vencimientos.

Fuentes de eventos automaticos:
- **Cuota por vencer/vencida**: `cronograma_cuota` (estado='PENDIENTE') JOIN `operacion` por
  tenant (la cuota no tiene tenant propio), `fecha_vencimiento <= hoy + dias_alerta`.
- **Contrato por vencer**: `operacion` estado='VIGENTE', `fecha_fin_contrato BETWEEN hoy AND
  hoy + dias_alerta`.
- **Promesa de pago**: REQ-0057 no existe aun -> se deja el tipo PROMESA y el vinculo, sin
  generador (se activara cuando exista promesa). Documentado como diferido parcial.

Dedup: `UNIQUE (tenant, tipo, origen_tabla, origen_id)` + `INSERT ... ON CONFLICT DO NOTHING`
para los automaticos. Las tareas manuales tienen origen NULL (no chocan con el indice).

## Archivos A Modificar

| Archivo | Cambio |
|---|---|
| dominio/agenda/AgendaEvento.java | NUEVO — entidad @Table("agenda_evento") con tenant |
| servicio/AgendaService.java | NUEVO — @AislarTenant: contar/listar (lazy), guardar, cerrar/reasignar, generarAutomaticos() |
| web/AgendaBean.java | NUEVO — @ViewScoped, LazyDataModel + filtros + CRUD manual |
| webapp/agenda.xhtml | NUEVO — listado con filtros (tipo/responsable/estado/fecha) + dialogo alta tarea |
| webapp/WEB-INF/plantilla.xhtml | item de menu "Agenda" |
| db/migration/V33__agenda_evento.sql | NUEVO — tabla + RLS inline + indice dedup + registra pantalla 'agenda' |
| servicio/InicioService.java | + KPIs "proximos vencimientos" y "tareas atrasadas" |
| web/InicioBean.java + webapp/index.xhtml | expone y renderiza esos KPIs |

## Pruebas Previstas

- [ ] Build mvn OK
- [ ] V33 en BEGIN...ROLLBACK contra BD real (crea tabla+RLS, registra pantalla, dedup)
- [ ] Backup pg_dump con RLS antes del deploy (V33 es esquema nuevo)
- [ ] Deploy + Flyway V33 success + smoke incluye `agenda`
- [ ] generarAutomaticos() no duplica al reabrir (ON CONFLICT)
- [ ] Aislamiento por tenant: un tenant no ve eventos de otro

## Riesgos

- Tabla de negocio nueva: DEBE llevar su RLS inline (V28 no cubre tablas futuras). Mitigado
  copiando el patron exacto de V29 (`documento_generado`).
- Generacion on-demand puede recalcular en cada apertura: mitigado con dedup por indice unico.
- Estados como varchar+CHECK (el proyecto NO usa @Enumerated); respetar ese patron.

## Cambios De Datos

V33: crea tabla `agenda_evento` (vacia) + politicas RLS + indice dedup + registra la pantalla
`agenda` en `entidad` (tenant -1). Sin tocar datos de negocio existentes.
