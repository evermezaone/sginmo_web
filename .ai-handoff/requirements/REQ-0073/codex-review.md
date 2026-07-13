# REQ-0073 - Revision Codex

**Fecha:** 2026-07-12  
**Auditor:** codex  
**Resultado:** REQUIERE_CAMBIOS

## Alcance revisado

- `.ai-handoff/requirements/REQ-0073/req.md`
- `.ai-handoff/requirements/REQ-0073/claude-implementation.md`
- `Desarrollo/sginmo-web/src/main/resources/db/migration/V50__objetivos_gerenciales.sql`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/ObjetivoService.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/ObjetivoBean.java`
- `Desarrollo/sginmo-web/src/main/webapp/objetivos.xhtml`

## Observaciones

### Obs 1 - Los periodos trimestral/anual/personalizado se guardan pero no se calculan

**Severidad:** alta  
**Archivo:** `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/ObjetivoService.java`

**Problema:** la UI permite elegir `MENSUAL`, `TRIMESTRAL`, `ANUAL` y `PERSONALIZADO`, pero `valorActual()` y `registrarMedicion()` usan siempre el mes actual (`inicioMes()` a `LocalDate.now()`). `DashboardMetricasService.valorMesActual()` tambien mide mes actual.

**Impacto:** un objetivo trimestral, anual o personalizado queda persistido con ese periodo, pero el semaforo, la brecha, el cumplimiento y el historial se calculan como mensual. Eso incumple el criterio de objetivos por periodo y puede llevar a decisiones gerenciales incorrectas.

**Solucion esperada:** centralizar la resolucion de rango por `periodo` y usarla en todos los indicadores y en `objetivo_medicion.periodo_desde/periodo_hasta`. Si `PERSONALIZADO` requiere fechas, la UI debe permitir cargarlas y el servicio validarlas.

### Obs 2 - El alcance prometido no se puede configurar desde la UI y solo se aplica parcialmente

**Severidad:** alta  
**Archivo:** `Desarrollo/sginmo-web/src/main/webapp/objetivos.xhtml`

**Problema:** la tabla tiene columnas `alcance` y `alcance_ref`, y el REQ declara EMPRESA/SUCURSAL/TIPO_ACTIVO/ZONA/PROPIETARIO/RESPONSABLE. Sin embargo, el dialogo del ABM no expone `alcance` ni `alcanceRef`; todos los objetivos nuevos quedan en `EMPRESA`. En el servicio, solo `SUCURSAL` tiene efecto mediante `alcanceSucursal()`, y ocupacion/rentabilidad/egresos ni siquiera aplican ese alcance.

**Impacto:** el usuario no puede crear objetivos por sucursal/tipo/zona/propietario/responsable, y aunque se cargaran por BD varios alcances no modifican el calculo. Incumple el criterio de alcance configurable y vuelve engañoso el dato guardado.

**Solucion esperada:** agregar controles de alcance en UI y aplicar el filtro en el motor para los alcances soportados. Para alcances aun no soportados, no deben aparecer como opciones configurables ni figurar como criterio cerrado.

### Obs 3 - El historial de mediciones se guarda pero no se puede ver

**Severidad:** alta  
**Archivo:** `Desarrollo/sginmo-web/src/main/webapp/objetivos.xhtml`

**Problema:** `registrarMedicion()` inserta en `objetivo_medicion`, pero la pantalla solo tiene un boton "Registrar medicion"; no hay vista, tabla, dialogo ni grafico para consultar el historial.

**Impacto:** incumple el criterio "Se guarda historial de mediciones por periodo para ver evolucion del objetivo". La evolucion queda en BD pero invisible para gerencia.

**Solucion esperada:** mostrar el historial del objetivo seleccionado con periodo, valor, cumplimiento, semaforo y fecha; idealmente en dialogo/pestaña o tabla expandible.

### Obs 4 - Los objetivos no tienen drill-down directo a evidencia

**Severidad:** alta  
**Archivo:** `Desarrollo/sginmo-web/src/main/webapp/objetivos.xhtml`

**Problema:** el criterio pide que cada objetivo tenga drill-down a evidencia. La pantalla muestra valor/brecha/semaforo y, para ocupacion, texto "faltan N prop.", pero no hay enlace a `dashboard-detalle`, `ocupacion` o la lista de vacantes.

**Impacto:** el usuario ve un desvio de objetivo pero no puede abrir desde ahi la evidencia que explica el valor. Esto rompe el flujo gerencial pedido: resumen -> evidencia -> accion.

**Solucion esperada:** agregar enlaces por indicador hacia la evidencia correspondiente: ocupacion/vacancia a ocupacion o `dashboard-detalle`, cobros/mora/ingresos/egresos/rentabilidad/contratos a claves whitelist de REQ-0074 cuando existan.

## Resultado

No apruebo `REQ-0073`. La base de tablas y calculo mensual inicial esta encaminada, pero faltan criterios centrales de periodo, alcance, historial visible y evidencia accionable.
