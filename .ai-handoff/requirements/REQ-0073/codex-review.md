# REQ-0073 - Revision Codex

**Fecha:** 2026-07-13  
**Auditor:** codex  
**Resultado:** REQUIERE_CAMBIOS

## Alcance revisado

- `.ai-handoff/requirements/REQ-0073/req.md`
- `.ai-handoff/requirements/REQ-0073/codex-review.md` anterior
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/ObjetivoService.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/ObjetivoBean.java`
- `Desarrollo/sginmo-web/src/main/webapp/objetivos.xhtml`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/DashboardDetalleBean.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/DrilldownService.java`

## Resultado de re-auditoria

Hay avances sobre las observaciones anteriores:

- `ObjetivoService.rangoPeriodo()` ya calcula mensual/trimestral/anual/personalizado y se usa en `valorActual()` y `registrarMedicion()`.
- La UI ya expone `alcance` y limita opciones a `EMPRESA`/`SUCURSAL`, evitando ofrecer alcances que aun no tienen efecto.
- Existe dialogo de historial con `objetivo_medicion`.
- Se agrego enlace de evidencia desde la grilla.

Pero todavia quedan hallazgos bloqueantes.

## Observaciones

### Obs 1 - El enlace de evidencia no envia el rango del objetivo y rompe indicadores de periodo

**Severidad:** alta  
**Archivo:** `Desarrollo/sginmo-web/src/main/webapp/objetivos.xhtml`

**Problema:** el link de evidencia envia `clave`, `hasta`, `sucursal` y `moneda`, pero no envia `desde`. `DashboardDetalleBean` pasa `desde=null` a `DrilldownService`. Para claves como `cobros` y `egresos`, el servicio ejecuta consultas con `BETWEEN :d AND :h`; con `:d` nulo la evidencia queda vacia o invalida. Ademas, aunque el objetivo sea trimestral/anual/personalizado, el link siempre usa `hasta=hoy` y no el mismo rango calculado para el objetivo.

**Impacto:** el usuario ve un objetivo desviado, pero el click de evidencia no muestra la base real del calculo. Esto incumple el flujo gerencial pedido: resumen -> evidencia -> accion. Tambien deja inconsistente el valor calculado contra el detalle visible.

**Solucion esperada:** exponer desde/hasta calculados por objetivo en el DTO o en el bean y enviar ambos parametros al `dashboard-detalle`. El rango debe ser el mismo que usa `ObjetivoService.rangoPeriodo()`. Para indicadores sin detalle soportado, no mostrar el boton o implementar la clave whitelist correspondiente.

### Obs 2 - El periodo PERSONALIZADO no permite cargar fechas al cambiar el combo

**Severidad:** media  
**Archivo:** `Desarrollo/sginmo-web/src/main/webapp/objetivos.xhtml`

**Problema:** el bloque de fechas esta renderizado con `rendered="#{objetivoBean.edicion.periodo eq 'PERSONALIZADO'}"`, pero el combo `Periodo` no tiene `p:ajax` que actualice ese bloque. Al abrir un objetivo mensual y cambiar a `PERSONALIZADO`, las fechas no aparecen en el dialogo. El usuario puede guardar un objetivo personalizado sin rango visible; el servicio termina usando `vigenciaDesde` por defecto y `vigenciaHasta` nulo.

**Impacto:** aunque el backend tenga `rangoPeriodo()`, la UI no permite configurar correctamente un periodo personalizado en el flujo normal. Eso incumple el criterio de objetivos por periodo personalizado.

**Solucion esperada:** envolver el bloque de fechas en un componente con `id` estable y actualizarlo desde el combo de periodo. Validar en backend que `PERSONALIZADO` tenga `vigenciaDesde` y `vigenciaHasta` coherentes, o definir explicitamente el comportamiento si `vigenciaHasta` queda abierto.

### Obs 3 - El historial usa `f:convertDateTime` sin `type="localDate"`

**Severidad:** media  
**Archivo:** `Desarrollo/sginmo-web/src/main/webapp/objetivos.xhtml`

**Problema:** `m.periodoDesde` y `m.periodoHasta` son `java.time.LocalDate`, pero el historial usa `<f:convertDateTime pattern="dd/MM/yyyy"/>` sin `type="localDate"`. En el resto de pantallas nuevas se corrigio este patron porque falla al renderizar tipos `LocalDate`.

**Impacto:** la vista de historial puede fallar o renderizar incorrectamente justo en la funcionalidad agregada para cerrar la observacion anterior.

**Solucion esperada:** usar `<f:convertDateTime type="localDate" pattern="dd/MM/yyyy"/>` para ambos campos.

## Resultado

No apruebo `REQ-0073`. La correccion va en buena direccion, pero aun no cumple evidencia accionable ni periodo personalizado desde UI, y tiene un riesgo de render en historial.
