# Estado Del Proyecto — SGInmo Web (SGI)

Actualizado: 2026-07-03

## Carpeta oficial

`C:\Users\everm\OneDrive\Documents\Datos\Sistemas\2R\Desarrollo\SGInmo\codigo fuente\inmobiliaria\Pysistemas\migracion`

- Código nuevo: `Desarrollo\sginmo-web\`
- Legado (solo lectura): `..\Inmobiliaria\`
- Docs de reglas de negocio: `docs-migracion\` (00-07)
- Herramientas: `herramientas\apache-maven-3.9.9\`

## Protocolo vigente

- Base de coordinación compartida `u237417599_project`, `PROJECT_CODE=SGI` (registrado 2026-07-03, IdProyecto=5, agentes: claude, codex, codex-spark, watcher, dashboard).
- BD-first: leer trabajo con `sp_siguiente_accion_agente('SGI', agente)`; transiciones con `sp_derivar_req`; archivos `.ai-handoff/` como mirror.
- Contrato completo en `.ai-handoff/WORKFLOW.md`; estándares en `.ai-handoff/standards/`.
- Claude implementa y deriva; solo Codex cierra.

## Estado reciente

- **Fase 0 CERRADA**: análisis completo del legado (docs 00-05), stack decidido (doc 06: WildFly 40 + PrimeFaces 15 + PostgreSQL + JasperReports), BD real analizada (doc 07: DOMINIOS, 25 SPs RPT_*, volúmenes, calidad OK).
- Alcance decidido por el usuario: migrar TODO lo programado en el legado, tenga o no datos hoy.
- Maven 3.9.9 portable instalado; JDK 23 local (target 21). PostgreSQL local: PENDIENTE de provisionar.
- **Backlog cargado (2026-07-03)**: 32 REQs (REQ-0001..0032) creados en la BD de coordinación bajo SGI (todos NUEVO/claude) y sus carpetas en `.ai-handoff/requirements/`. Definición y fuentes en `docs-migracion\08-backlog-reqs.md`.
- Próximo paso: desarrollar REQ-0001 (esqueleto Maven/WildFly/PostgreSQL/Flyway). `req.md` y `analysis.md` de cada REQ se completan al tomarlo (EN_ANALISIS).

## Nota operativa

Este archivo resume contexto para retomar sesiones; no reemplaza la BD de coordinación ni `WORKFLOW.md`.
