# REQ-0052 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0052
- Tipo de cambio: BD (tabla nueva + RLS) + backend + UI
- Riesgo: medio (tabla de negocio nueva con RLS; escritura por tenant; INSERT nativo con dedup)
- Archivos clave:
  - `resources/db/migration/V33__agenda_evento.sql`: tabla `agenda_evento` por-tenant + RLS inline (patron documento_generado V29) + indice unico parcial de dedup + registra pantalla `agenda`.
  - `dominio/agenda/AgendaEvento.java`: entidad (extiende Auditable); estados varchar (no @Enumerated).
  - `servicio/AgendaService.java`: @AislarTenant; contar/listar (lazy), guardar, cambiarEstado, reasignar, generarAutomaticos() (INSERT nativo con ON CONFLICT DO NOTHING). Autorizacion backend en escrituras.
  - `web/AgendaBean.java`: @ViewScoped, LazyDataModel + filtros + alta/edicion/cerrar; genera automaticos al abrir.
  - `webapp/agenda.xhtml`: listado lazy con filtros + dialogo de tarea (scroll interno + pie fijo).
  - `webapp/WEB-INF/plantilla.xhtml`: item de menu "Agenda" (seccion Operaciones).
  - `servicio/InicioService.java` + `web/InicioBean.java` + `webapp/index.xhtml`: KPIs "proximos vencimientos" y "tareas atrasadas".
  - `tools/smoke-test-vps.py`: agrega `agenda`.
- Comandos probados:
  - `mvn -q clean package -DskipTests`: BUILD OK.
  - V33 en `BEGIN...ROLLBACK` contra BD real: DDL + RLS + pantalla OK; dedup verificado (2 inserts mismo origen -> 1 fila).
  - Deploy + Flyway V33 `success=t`; pantalla `agenda` registrada.
  - `python tools/smoke-test-vps.py`: 20/20 RENDER OK incluida `agenda`.
- Cambios de datos: si, V33 crea tabla `agenda_evento` (vacia) + registra pantalla. Sin tocar datos existentes.
- Cambios de entorno: no.
- Impacto LLM/tokens: no.
- Decision esperada: revisar riesgo puntual (RLS de tabla nueva + INSERT nativo con dedup) y las limitaciones documentadas.
- Notas para auditor:
  - Verificar la RLS de `agenda_evento` (per-tenant, sin filas globales) y que `generarAutomaticos()` no genera en contexto global (-1).
  - Verificar el dedup: indice unico parcial `(tenant, tipo, origen_tabla, origen_id) WHERE origen_id IS NOT NULL` + `ON CONFLICT` (no duplica al reabrir).
  - Bug corregido en esta entrega: `Map.of(...)` es inmutable y lanza NPE con clave null; `AgendaService.listar` ahora guarda el orden null.

## Resumen Funcional

Nueva "Agenda" (menu Operaciones): lista de eventos con filtros (tipo/estado/busqueda), alta/edicion
de tareas manuales (tipo, titulo, fecha, prioridad, estado, responsable, descripcion), y generacion
automatica de vencimientos (cuotas PENDIENTE por vencer/vencidas y contratos VIGENTE proximos a vencer),
sin duplicar al reabrir. El tablero de inicio suma "proximos vencimientos (7 dias)" y "tareas atrasadas".

## Resumen Tecnico

Tabla `agenda_evento` por-tenant con RLS inline. `AgendaService` @AislarTenant expone lazy list +
escrituras con autorizacion backend; `generarAutomaticos()` hace dos INSERT nativos idempotentes
(dedup por indice unico parcial). Estados como varchar+CHECK. Generacion on-demand al abrir la agenda
y el tablero (no hay scheduler EJB; documentado como mejora futura).

## Limitaciones Conocidas (transparencia para el auditor)

- Vinculo manual de tarea a persona/activo/operacion/cobro: soportado en el MODELO (columnas FK), pero
  el dialogo aun no expone selectores de esas entidades. Refinamiento menor.
- Filtro por responsable y por rango de fecha: el servicio soporta responsable; faltan controles UI
  dedicados (hoy: filtros tipo/estado + busqueda por texto). Refinamiento menor.
- Promesas de pago automaticas: dependen de REQ-0057 (inexistente); el tipo PROMESA y el vinculo ya
  estan, sin generador. Diferido.
- Sucursal: no se aplica en esta iteracion (solo tenant/empresa).

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| V33__agenda_evento.sql | NUEVO — tabla + RLS + dedup + pantalla |
| dominio/agenda/AgendaEvento.java | NUEVO — entidad |
| servicio/AgendaService.java | NUEVO — logica agenda + automaticos |
| web/AgendaBean.java | NUEVO — backing bean lazy |
| webapp/agenda.xhtml | NUEVO — vista |
| webapp/WEB-INF/plantilla.xhtml | item de menu Agenda |
| servicio/InicioService.java | KPIs proximos vencimientos / tareas atrasadas |
| web/InicioBean.java | genera automaticos + expone KPIs |
| webapp/index.xhtml | 2 tarjetas KPI |
| tools/smoke-test-vps.py | cobertura de `agenda` |

## Cambios De Datos

V33: crea `agenda_evento` (vacia) con RLS per-tenant + indice de dedup; registra la pantalla `agenda`
en `entidad` (tenant -1). Requiere `set_config('app.tenant','-1',true)` por RLS (V28).

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Ver `test-plan.md`. Build OK; V33 validada en rollback (dedup verificado); deploy + Flyway success;
smoke 20/20 RENDER OK. Bug NPE (Map inmutable) detectado por el smoke y corregido antes de cerrar.

## Pruebas Manuales Sugeridas

1. Menu Operaciones -> Agenda: crear una tarea, verla en la lista, cerrarla.
2. Con cuotas/contratos proximos a vencer en una empresa: abrir la agenda -> aparecen VENCIMIENTOs; reabrir -> no se duplican.
3. Tablero de inicio: ver "proximos vencimientos" y "tareas atrasadas".

## Riesgos Conocidos

- Generacion on-demand hace INSERTs en cada apertura (mitigado por dedup; costo bajo).
- Ver "Limitaciones Conocidas".
