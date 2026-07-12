# REQ-0052 - Analisis

**Estado:** EN_ANALISIS
**Fecha:** 2026-07-12
**Analista:** Claude

## Analisis Funcional

Agenda operativa con eventos de dos origenes: manuales (tareas del usuario, vinculables a
persona/activo/operacion/cobro) y automaticos (vencimiento de cuotas y contratos proximos a
vencer). Listado con filtros (tipo, responsable, estado, fecha) y alta/edicion/cierre de tareas.
El tablero de inicio muestra "proximos vencimientos" y "tareas atrasadas". Todo por empresa (tenant).

## Analisis Tecnico

Ver `claude-plan.md`. Tabla nueva `agenda_evento` por-tenant con RLS inline (patron V29
`documento_generado`), migracion V33. Generacion automatica ON-DEMAND con dedup por indice unico
`(tenant, tipo, origen_tabla, origen_id)` + `ON CONFLICT DO NOTHING` (no duplica al reabrir).
Servicio `@AislarTenant` + LazyDataModel. Estados como `varchar + CHECK` (el proyecto no usa
`@Enumerated`). Se extienden `InicioService.Kpis` + `index.xhtml` para el tablero.

## Decisiones de alcance tomadas (sin consultar; modo autonomo)

- **Generacion automatica**: ON-DEMAND al abrir la agenda/tablero (no hay scheduler EJB en el
  proyecto). Se deja documentado `@Schedule` como mejora futura. Evita introducir infra de timers
  esta noche y cumple el criterio de eventos automaticos sin duplicar (dedup por indice).
- **Promesas de pago**: REQ-0057 no existe aun. Se incluye el tipo `PROMESA` y el vinculo opcional,
  pero SIN generador automatico de promesas (se activara con REQ-0057). Diferido parcial documentado.
- **Dias de alerta** (cuota/contrato por vencer): constante por defecto 30 dias (se movera a
  parametros con REQ-0060). Evita bloquear por dependencia.
- **Estados del evento**: `PENDIENTE | EN_CURSO | RESUELTO | CERRADO` (varchar+CHECK). Prioridad
  `BAJA | MEDIA | ALTA`. Tipos `RECORDATORIO | TAREA | VENCIMIENTO | PROMESA`.

## Riesgos

| Riesgo | Probabilidad | Impacto | Mitigacion |
|---|---|---|---|
| Tabla nueva sin RLS (V28 no cubre futuras) | media | alto | RLS inline copiando patron V29 documento_generado |
| Duplicar eventos automaticos al reabrir | media | medio | indice unico + ON CONFLICT DO NOTHING |
| Cuota sin tenant propio | baja | medio | aislar por el tenant del evento; JOIN a operacion al generar |

**Semaforo Codex:** medio

## Preguntas Abiertas

- [x] Ninguna (todas las decisiones de alcance resueltas arriba; el usuario verifica al dia siguiente).

## Impacto En Costos / LLM

- Aumenta tokens por mensaje: no
- Agrega llamadas extra al LLM: no
- Puede resolverse sin IA: si

## Impacto En Datos

- Requiere migracion: si (V33 crea `agenda_evento` + RLS + registra pantalla `agenda`)
- Tablas/colecciones afectadas: `agenda_evento` (nueva); lectura de `cronograma_cuota`/`operacion`

## Recomendacion

**Desarrollar** — riesgo medio, valor vendible (seguimiento de cartera y vencimientos).
