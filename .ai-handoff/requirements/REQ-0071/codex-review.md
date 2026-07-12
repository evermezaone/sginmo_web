# REQ-0071 - Auditoria Codex

Fecha: 2026-07-12
Auditor: codex
Decision: REQUIERE_CAMBIOS

## Alcance revisado

- `.ai-handoff/requirements/REQ-0071/req.md`
- `.ai-handoff/requirements/REQ-0071/claude-implementation.md`
- `.ai-handoff/requirements/REQ-0071/test-plan.md`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/RentabilidadService.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/RentabilidadBean.java`
- `Desarrollo/sginmo-web/src/main/webapp/rentabilidad.xhtml`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/DrilldownService.java`

## Verificacion

- `mvn -q clean package` en `Desarrollo`: OK durante esta ronda de auditoria.
- Inspeccion de codigo real: completada.

## Hallazgos

### Obs 1 - Los montos no permiten abrir evidencia desde la pantalla

Problema: `RentabilidadService` genera `drillKey` para lineas y activos, pero `rentabilidad.xhtml` no usa esos keys: las tablas de ingresos, egresos y ranking renderizan texto plano, sin `h:link`/boton hacia `dashboard-detalle` ni parametros de periodo. Ademas, los keys `ingreso_egreso:TIPO:APLICACION` no estan en la whitelist de `DrilldownService`; hoy solo existen claves generales `ingresos`, `egresos` y `rentabilidad_activo`.

Impacto: incumple el criterio "Cada monto permite abrir evidencia (drill-down 0074)". Gerencia ve un monto agregado por tipo/articulo, pero no puede abrir el detalle que lo explica desde esa linea.

Solucion esperada: enlazar cada linea de ingreso/egreso y cada activo del ranking a una evidencia funcional. Para tipos, o bien extender `DrilldownService` con filtro por tipo/aplicacion, o usar claves existentes `ingresos`/`egresos` agregando parametros que filtren aplicacion. Para ranking, usar `rentabilidad_activo` con `refId=activoId` y periodo.

Evidencia:
- `RentabilidadService.java:60-79`
- `RentabilidadService.java:82-107`
- `rentabilidad.xhtml:37-60`
- `DrilldownService.java:27-65`

### Obs 2 - El ranking no garantiza mostrar mejores y peores activos

Problema: `rankingActivos()` ordena por `neto DESC` y aplica `setMaxResults(limite)`. Con muchos activos, esto devuelve solo los mejores N; los peores quedan fuera. La pantalla presenta una sola tabla y no separa mejores/peores.

Impacto: incumple el criterio "Incluye ranking de mejores y peores activos por rentabilidad neta". En una cartera grande, los activos deficitarios pueden no aparecer, que son justamente los que gerencia necesita detectar.

Solucion esperada: devolver dos rankings separados (mejores y peores), o una estructura que garantice incluir ambos extremos. La UI debe mostrar claramente mejores y peores activos, con evidencia por activo.

Evidencia:
- `RentabilidadService.java:82-107`
- `rentabilidad.xhtml:54-60`

## Resultado

No apruebo REQ-0071. El calculo base de ingresos/egresos/neto esta encaminado, pero faltan criterios funcionales clave de evidencia y ranking de peores activos.
