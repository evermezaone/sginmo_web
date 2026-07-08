# Instrucciones para Claude Code — Proyecto SGInmo Web (SGI)

## Al iniciar cada sesión — OBLIGATORIO

Consultar la base de coordinacion como primer paso:

```sql
CALL sp_siguiente_accion_agente('SGI', 'claude');
```

Este procedure devuelve dos result sets:

1. **Mensajes de chat no leídos** en el canal `claude`:
   - Responder con `sp_responder_chat('SGI', 'claude', 'assistant', 'claude', respuesta)`.
   - Marcar leídos con `sp_marcar_chat_leido('SGI', 'claude')`.
   - Si el mensaje es una tarea o comando, ejecutarlo además de responder.

2. **REQs asignados a `claude`** que no están `CERRADO` ni `CANCELADO`, ordenados por prioridad:

| EstadoOperativo / Estado | Acción |
|--------|--------|
| `REQUIERE_CAMBIOS` | Leer observaciones en `AUDITORIA_OBSERVACION` → corregir → derivar con `sp_derivar_req('SGI', req, 'LISTO_PARA_REVISION', 'codex', 'claude', resumen)` |
| `BLOQUEADO_POR_USUARIO` | Escalar al usuario para decisión |
| `ESPERA_USUARIO` | Verificar si el usuario ya respondió; si sí, retomar |
| `EN_DESARROLLO` | Continuar implementación → derivar a `LISTO_PARA_REVISION` al finalizar |
| `NUEVO` / `EN_ANALISIS` | Analizar, implementar y derivar |
| `BLOQUEADO_POR_PRIORIDAD` | No iniciar hasta resolver el REQ bloqueante |

**Si la BD no está disponible**, leer `.ai-handoff/to_claude.md` como respaldo de compatibilidad y registrar el fallo antes de continuar.

**La BD manda sobre los archivos.** Si hay contradicción entre la BD (`REQ.Estado`, `REQ.Responsable`), `registry.jsonl` y el resumen de sesión anterior, la BD es la fuente de verdad.

## Acceso MySQL rutinario (base de coordinación)

No ejecutar SQL inline/ad-hoc. Flujo obligatorio con runner:

1. Escribir las sentencias/`CALL` en `mysql_runner.sql` (en `migracion/`).
2. Ejecutar: `mysql --defaults-extra-file=.\tmp_my.cnf --batch --raw --execute="source mysql_runner.sql"`
3. Renombrar a histórico `mysqlYYYYMMDDHHmmss.sql` (queda en el workspace como trazabilidad).
4. Para la siguiente operación, crear un nuevo `mysql_runner.sql`.

`tmp_my.cnf` es solo el archivo de credenciales del cliente; el runner es `mysql_runner.sql`.

## Autoridad de cierre de REQs

Claude desarrolla, corrige, documenta y deriva REQs a revision, pero **no aprueba ni cierra** requerimientos.

- Claude puede derivar a `LISTO_PARA_REVISION` con `responsable='codex'`.
- Claude no debe llamar `sp_derivar_req(..., 'CERRADO', ...)` ni marcar `APROBADO_POR_CODEX`.
- Solo Codex auditor cierra (`CERRADO`/`APROBADO_POR_CODEX`) después de auditar.
- Codex-Spark solo corrige observaciones puntuales asignadas y devuelve a Codex.
- Una decisión funcional del usuario se documenta y se deriva a revisión; no reemplaza la auditoría.

Antes de enviar cualquier REQ a Codex: aplicar el checklist completo de `.ai-handoff/standards/audit-checklist.md`, completar `preaudit-checklist.md`, cerrar/justificar observaciones en `AUDITORIA_OBSERVACION`, y ejecutar `npm run handoff:check` + `npm run handoff:ready -- REQ-XXXX` (única vía normal de envío). Cuando un REQ vuelve de `REQUIERE_CAMBIOS`, responder observación por observación con el bloque `Obs NN` (problema original / cambio aplicado / archivos / evidencia / validación propia).

### Disenso técnico fundamentado

Claude no acata ciegamente las observaciones de Codex ni las definiciones de tareas: si conoce una solución mejor, **debe proponerla y fundamentarla** (decisión del usuario, 2026-07-04).

Reglas:

1. Toda observación de Codex SE RESPONDE siempre — nunca se ignora. Las respuestas válidas son dos: **corregir** (camino normal) o **contraproponer con fundamentos**.
2. Una contrapropuesta debe citar evidencia verificable: docs de reglas (`docs-migracion/`), estándares del proyecto, comportamiento del legado, documentación oficial del stack, o una prueba reproducible. "Me parece mejor" no es fundamento.
3. Mecánica: documentar la contrapropuesta en el bloque `Obs NN` del `preaudit-checklist.md` (problema original / por qué la solución pedida no es la mejor / propuesta alternativa / evidencia), cerrar la observación en BD como `descartada` o `diferida` con la `Resolucion` explicando el fundamento, y reenviar con la compuerta. Codex re-audita el argumento por sus méritos.
4. Si Codex insiste y el desacuerdo persiste tras una ronda, no se cicla: se escala al usuario (`ESPERA_USUARIO`) presentando AMBAS posiciones con sus fundamentos, y decide él.
5. Lo mismo aplica a la definición de REQs/tareas: si el alcance o el diseño planteado (por Codex, por el backlog o por una decisión previa) tiene una alternativa mejor, Claude la propone fundamentada en `analysis.md` antes de implementar; los cambios de semántica de negocio siempre los decide el usuario.

### Revisión transversal de flujos equivalentes

Si una corrección toca una regla de negocio, validación, estado, entidad JPA, servicio, converter, plantilla o componente compartido, buscar y corregir TODOS los flujos equivalentes del proyecto (ej.: si cambia la regla de estado de cuotas, revisar cobro, anulación, regeneración, liquidación y ETL). Documentar la búsqueda en `test-plan.md` o `preaudit-checklist.md`.

## Regla de continuidad y prioridad

- Trabajar siempre primero el menor REQ pendiente del lado de Claude.
- Lotes de máximo 5 REQs (más solo con `BATCH_GRANDE_APROBADO_POR_USUARIO`).
- Mientras Codex audita, Claude desarrolla el siguiente REQ; cuando Codex responde, se intercala la corrección con prioridad.
- Toda regla repetible aprendida → `.ai-handoff/standards/` + aplicarla a los REQs pendientes.

## Contexto del Proyecto

**Origen (legado):** SGInmo — ERP inmobiliario de escritorio. C# WinForms, .NET Framework 4.0, EF5 (EDMX database-first), **Firebird 2.5** (`INMOBILIARIA.FDB`), Crystal Reports. Código en `Pysistemas\Inmobiliaria\`.
**Destino:** **WildFly 40** (Jakarta EE 11, Java 21 LTS) + **JSF/Faces 4.1 + PrimeFaces 15** + **CDI/`@Transactional`** + **JPA 3.2 (Hibernate)** + **PostgreSQL 16** + **Jakarta Security integrada (bcrypt)** + **JasperReports** + **Maven**.
**Código nuevo:** `migracion\Desarrollo\sginmo-web\` (WAR único).
**Herramientas:** JDK 23 local (compilar con `--release 21`), Maven portable en `migracion\herramientas\apache-maven-3.9.9\bin\mvn.cmd`. Firebird 2.5.9 embebido portable disponible para consultar la BD legada (copia, nunca el original).

**Alcance decidido por el usuario:** se migra TODO lo programado en el legado (alquiler, venta, cobros, liquidaciones, rescisiones, renovaciones, ingresos/egresos, imágenes, reportes), independiente de si hoy tiene datos.

### Fuente de verdad de las reglas de negocio

La documentación de análisis en `docs-migracion/` (junto al legado, `migracion\docs-migracion\`):

| Doc | Contenido |
|---|---|
| `00-INDICE.md` | Resumen, máquinas de estado, riesgos |
| `01-stack-actual.md` | Stack legado |
| `02-modelo-datos.md` | 31 tablas + vistas + relaciones + enums |
| `03-reglas-negocio-nucleo.md` | Reglas RN-* de cobros/operaciones/cuotas/liquidaciones |
| `04-servicios-y-logica.md` | Fórmulas de services + deuda técnica |
| `05-soporte-seguridad-reportes.md` | Seguridad, parámetros, catálogo de reportes |
| `06-propuesta-stack-web.md` | Stack definido + mapeo legado→nuevo |
| `07-datos-reales.md` | BD real: DOMINIOS, SPs RPT_*, volúmenes |

**Trazabilidad obligatoria:** toda regla de negocio implementada debe citar su fuente: ID `RN-*` del doc 03/04, archivo C# del legado (`FrmX.cs` / `XService.cs`), SP de Firebird (`RPT_*`), valor real de `DOMINIOS` (doc 07), o decisión explícita del usuario.

### Bugs del legado que NO deben replicarse

1. Sin transacciones multi-tabla (cobro, operación+cuotas, liquidación, anulación) → usar `@Transactional`.
2. Renovación duplica cuotas (bug reconocido en OperacionesService, rama "R").
3. Redondeo `ToString("N0")` que trunca → usar `BigDecimal` con regla de ajuste a la última cuota.
4. Moneda hardcodeada (MONEDA_ID=1) → parametrizar (o decisión explícita de operar solo Gs.).
5. Contraseñas Base64 reversibles → bcrypt/PBKDF2 + rate limiting.
6. Autorización solo en UI → `@RolesAllowed`/policies en backend siempre.
7. Estado de propiedad seteado aparte (SP_CORRIGE_ESTADO_PROPIEDADES existe porque se desincronizaba) → derivarlo de la operación con invariantes transaccionales.

### Convenciones obligatorias

- Lógica de negocio en servicios CDI `@ApplicationScoped` + `@Transactional`; los beans JSF (`@ViewScoped`) solo orquestan UI. Nada de reglas en `.xhtml`.
- JPA parametrizado siempre (named queries / criteria); nada de SQL concatenado.
- Estados y tipos como **enums Java** (`@Enumerated(STRING)`) con los valores reales del doc 07 §3.
- Montos: `BigDecimal` (`NUMERIC(15,2)` en PostgreSQL).
- Auditoría (usuario/fecha creación/modificación): listener JPA global, no manual.
- Multi-tenant: contexto empresa/sucursal en sesión (equivalente de VARIABLES_ENTORNOS); filtro por `empresa_id` en consultas.
- Credenciales solo en `.env`/configuración de WildFly (datasource JNDI `java:/jdbc/SGInmoDS`), nunca en código.
- Migraciones de esquema PostgreSQL: **Flyway** (`db/migration/V###__*.sql`), idempotentes, incluidas en el mismo REQ que el código dependiente.
- Verificación mínima por REQ: `mvn -q package` con EXIT:0 documentado en `claude-implementation.md`; tests con `mvn test` cuando el REQ incluye lógica de negocio.

## Protocolo completo

Leer `.ai-handoff/WORKFLOW.md` y `.ai-handoff/standards/` al iniciar cada sesión.

## Modo de Operacion Autonomo — ACTIVO

Autorización permanente para proceder sin pedir confirmación en:

- Crear, editar y sobreescribir cualquier archivo del proyecto.
- Ejecutar SQL en la base de coordinación (flujo `mysql_runner.sql`) y en PostgreSQL local de desarrollo.
- Consultar la BD Firebird legada (siempre sobre copia).
- Derivar REQs a revisión; no cerrar sin auditoría de Codex.
- Instalar dependencias (Maven, npm para tooling de handoff).

**Solo pausar si:**
- DELETE masivo o DROP sobre datos reales existentes.
- El ETL de datos legados está listo para ejecutarse contra la BD definitiva (avisar antes).
- Error o decisión de diseño/negocio que requiere al usuario.

Para todo lo demás: **implementar directamente**.

## Control remoto — Modo loop

```
/loop En cada vuelta:
1. Ejecutar CALL sp_siguiente_accion_agente('SGI', 'claude') para obtener mensajes de chat no leídos y REQs pendientes asignados a claude.
— Si hay mensajes de chat no leídos: responder con sp_responder_chat, luego marcar leídos con sp_marcar_chat_leido. Si el mensaje es tarea o comando, ejecutarlo además de responder.
— Si hay REQs pendientes (Responsable=claude, Estado distinto de CERRADO/CANCELADO, EstadoOperativo distinto de BLOQUEADO_POR_PRIORIDAD): actuar según lo acordado.
— Si no hay nada pendiente: no hacer nada.
2. Esperar con un único sleep 90 y volver a empezar.
```

## Comandos de PowerShell

Para evitar confirmaciones de seguridad:

- No usar subexpresiones `$()` en comandos ni strings.
- No interpolar variables en strings con comillas dobles (`"texto $variable"`).
- Mostrar texto + variable con comas (`Write-Output "Exit:", $LASTEXITCODE`) o concatenación (`+`).
- Para lógica compleja, escribir un script `.ps1` y ejecutarlo.

## Base de Coordinacion Multiagente

Variables en `.env`: `PROJECT_DB_HOST/PORT/USER/PASS/NAME` + `PROJECT_CODE=SGI`. Misma base compartida con FLX/VLS/gestion-migracion (`u237417599_project`).

| Procedimiento | Uso |
|---|---|
| `sp_siguiente_accion_agente('SGI', agente)` | Chat no leído + REQs pendientes |
| `sp_derivar_req('SGI', req, estado, responsable, actor, resumen)` | Transición atómica |
| `sp_crear_req` / `sp_modificar_req` | Alta/edición de REQs |
| `sp_responder_chat` / `sp_marcar_chat_leido` | Chat |
| `sp_registrar_observacion` | Observaciones de auditoría |

Si BD y archivos discrepan, manda la BD. Si la BD no está disponible, registrar el fallo y usar los archivos.

## Loop de auditorias (Claude)

Cuando se corre un loop periodico que revisa el buzon (observaciones de Codex),
la condicion de PARADA es obligatoria: si TODOS los REQ del proyecto quedaron
respondidos (ninguno en REQUIERE_CAMBIOS/EN_CORRECCION y 0 observaciones
abiertas en AUDITORIA_OBSERVACION), Claude debe DETENER el loop (CronDelete /
omitir el proximo ScheduleWakeup) en vez de seguir consultando en vacio.
Se re-arma solo cuando el usuario lo pida o cuando haya una nueva tanda de
REQs/auditorias en curso.
