# REQ-0046 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-11
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0046
- Tipo de cambio: UI + backend (fix de bug de integridad; sin BD/migracion)
- Riesgo: medio
- Archivos clave:
  - `sginmo-web/src/main/webapp/operaciones.xhtml`: agrega el campo Moneda (linea 96-101) con
    `<p:selectOneMenu value="#{operacionBean.seleccionado.moneda}" required="true">` y
    `<f:selectItems value="#{operacionBean.monedas}" var="m" itemLabel="#{m.descripcion}"
    itemValue="#{m.id}"/>`.
  - `sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/OperacionBean.java`: campo
    `List<Moneda> monedas` (linea 59), carga `monedas = catalogoService.monedasActivas()` en el
    init (linea 74), getter `getMonedas()` (linea 181) y en `nuevo()` preselecciona por defecto
    Guaranies (`descripcion` contiene "guaran") o la primera visible (lineas 95-100).
  - `sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/CatalogoService.java`: metodo
    `monedasActivas()` (linea 79) = `SELECT m FROM Moneda m WHERE m.estado = 'ACTIVO' AND
    (m.tenant = -1 OR m.tenant = :t) ORDER BY m.descripcion`.
- Comandos probados:
  - `mvn -q clean package -DskipTests`: BUILD OK.
  - `python tools/smoke-test-vps.py`: 19/19 pantallas RENDER OK, incluida `operaciones`.
- Cambios de datos: no (usa el catalogo `Moneda` existente; sin migracion). Se empieza a
  escribir `operacion.moneda` en altas nuevas, que antes quedaba null y rompia el INSERT.
- Cambios de entorno: no.
- Impacto LLM/tokens: no.
- Decision esperada: aprobar (revisar el riesgo puntual de integridad de moneda: `required` +
  default Guaranies + fallback a primera moneda visible).
- Notas para auditor:
  - `monedasActivas()` respeta el patron multi-tenant (globales -1 + propias del tenant), igual
    que los demas metodos de catalogo (p.ej. `formasHabilitadas()`).
  - Si no hubiera ninguna Moneda ACTIVA, `required="true"` bloquea el guardado con mensaje en
    vez de intentar persistir null. Recomendacion operativa: tener Guaranies sembrada como
    global (tenant -1) y ACTIVA.

## Resumen Funcional

El formulario de alta/edicion de operacion ahora tiene un selector de Moneda obligatorio. Al
crear una operacion nueva se preselecciona Guaranies (o la primera moneda visible), de modo
que el registro ya no falla con el error de integridad "null value in column moneda".

## Resumen Tecnico

Se agrega el metodo de catalogo `monedasActivas()` (Moneda ACTIVO filtrada por tenant -1 o
propio). `OperacionBean` carga esa lista, la expone via `getMonedas()` y en `nuevo()` fija la
moneda por defecto buscando la descripcion que contiene "guaran", con fallback a la primera de
la lista. En `operaciones.xhtml` el `p:selectOneMenu required="true"` liga la seleccion a
`seleccionado.moneda`. Sin migracion: usa el catalogo Moneda existente.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| webapp/operaciones.xhtml | `p:selectOneMenu` de Moneda obligatorio con `#{operacionBean.monedas}` |
| web/OperacionBean.java | Lista `monedas` + carga `monedasActivas()` + default Guaranies en `nuevo()` + getter |
| servicio/CatalogoService.java | NUEVO metodo `monedasActivas()` (Moneda ACTIVO, tenant -1 o propio) |

## Cambios De Datos

Sin migracion. En altas nuevas se persiste `operacion.moneda` (antes quedaba null y fallaba el
INSERT por NOT NULL). No altera operaciones existentes.

## Variables De Entorno

Ninguna.

## Pruebas Ejecutadas

Ver `test-plan.md`. Resumen: build OK, smoke-test 19/19 RENDER OK incluida `operaciones`; alta
de operacion se guarda sin error de integridad por moneda.

## Pruebas Manuales Sugeridas

1. Login → Operaciones → Nuevo: verificar que el campo Moneda aparece preseleccionado en
   Guaranies y es obligatorio; guardar y confirmar que se registra sin error.
2. Intentar guardar dejando Moneda vacia (si se limpia): debe bloquear con mensaje `required`.

## Riesgos Conocidos

- Si el tenant no tiene ninguna Moneda ACTIVA, el combo queda vacio y `required` bloquea el
  guardado. Mitigacion operativa: sembrar Guaranies como global (tenant -1) ACTIVA.
