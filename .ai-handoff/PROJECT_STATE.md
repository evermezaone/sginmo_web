# Estado Del Proyecto — SGInmo Web (SGI)

Actualizado: 2026-07-05

## Carpeta oficial

`C:\Users\everm\OneDrive\Documents\Datos\Sistemas\2R\Desarrollo\SGInmo\codigo fuente\inmobiliaria\Pysistemas\migracion`

- Código nuevo: `Desarrollo\sginmo-web\`
- Legado (solo lectura): `..\Inmobiliaria\`
- Docs de reglas de negocio: `docs-migracion\` (00-10; fuentes INE en `fuentes-ine-2022\`)
- Herramientas: `herramientas\apache-maven-3.9.9\` (+ WildFly 40 local); compuerta: `python tools/handoff.py ready SGI REQ-XXXX "..."`

## Protocolo vigente

- Base de coordinación compartida `u237417599_project`, `PROJECT_CODE=SGI` (IdProyecto=5; agentes: claude, codex, codex-spark, watcher, dashboard).
- BD-first: leer trabajo con `sp_siguiente_accion_agente('SGI', agente)`; transiciones con `sp_derivar_req`; archivos `.ai-handoff/` como mirror. SQL vía `mysql_runner.sql` + `tmp_my.cnf`.
- Claude implementa y deriva; solo Codex cierra. Disenso técnico fundamentado habilitado (CLAUDE.md/CODEX.md).
- Flujo con el usuario: preguntas de decisión DE A UNA; resumen + autorización al final.

## Estado reciente (2026-07-05)

- **REQ-0003 EN_DESARROLLO — revisión interactiva del esquema con el usuario. NADA se aplica a la BD hasta su OK final.**
- **V1 rev 6** (34 tablas + vista `v_persona`): singular, **PK = nombre de la tabla** (convención del usuario; FKs sin `_id`, texto = `descripcion`), modelo persona/persona_fisica/persona_juridica/persona_rol, **tabla `activo` recursiva** (reemplaza entidades_inmobiliarias+propiedades; tipo define + atributos por tipo complementan), tabla empresa ELIMINADA (empresa = persona_juridica; sucursal → persona_juridica), `articulo` absorbió items_ingresos_egresos y articulo_detalle, boolean `activo`→`estado` en todas, archivo_adjunto con FKs reales, `ubicacion_geografica.codigo_oficial` (INE, upsert futuro).
- **V2** seed básico: 6 parámetros reales (doc 07), ~20 listas `entidad`, 4 monedas, 3 impuestos IVA-PY, 5 formas de pago con flags, 15 artículos de servicio con `aplicacion`.
- **V3** ubicaciones Paraguay GENERADO desde XLSX oficiales INE 2022 (8.276 filas: país+18 dep+263 distritos+7.994 barrios; 0 duplicados; generador `gen_v3_ubicaciones.py` en scratchpad de la sesión).
- **Arquitectura BD-céntrica (decisión del usuario)**: consistencia del dinero EN LA BD — triggers de cuadre + SPs/funciones/vistas estilo Gestión (COMMIT explícito permitido donde el patrón lo requiera; numeración con `FOR UPDATE`); Java LLAMA a los SPs, prohibido reimplementar el cuadre. V4 motor documento y V5 motor cobro se harán en la fase dinero. Especificación fuente: doc 10 (auditoría del Oracle de Gestión: one@localhost/XE, credenciales en .env GESTION_DB_*).
- **VPS lista** (`ssh sginmo-vps`): WildFly 40 + JDK 21 corriendo con sginmo-web (localhost:8080; scripts en `~/apps/sginmo/`); PostgreSQL 16 con BD `sginmo` VACÍA (credenciales APP_DB_* en .env). Deploy: `powershell -File tools\deploy-vps.ps1`.
- **Cola Codex**: REQ-0000/0001/0002 en LISTO_PARA_REVISION (Obs 201 corregida y cerrada).
- Próximo al retomar: seguir recibiendo observaciones del usuario sobre V1/V2/V3; con su OK final → aplicar migraciones a la VPS (Flyway vía maven plugin o psql), documentar REQ-0003 y derivar. Trampas del entorno: `MAVEN_OPTS=-Djavax.net.ssl.trustStoreType=Windows-ROOT`; `JBOSS_HOME` global viejo (fijar siempre al WildFly 40 del proyecto); pipes de PS 5.1 corrompen encoding (usar archivo temporal + `cmd /c type | ssh`).

## Nota operativa

Este archivo resume contexto para retomar sesiones; no reemplaza la BD de coordinación ni `WORKFLOW.md`.
