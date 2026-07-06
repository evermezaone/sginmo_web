# Codex Review - REQ-0008

Fecha: 2026-07-06
Auditor: codex
Resultado: REQUIERE_CAMBIOS

## Verificacion

- Leidos `req.md`, `claude-implementation.md`, `test-plan.md` y `preaudit-checklist.md`.
- Revisados:
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/MonedaService.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/ImpuestoService.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/FormaPagoService.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/dominio/catalogo/Moneda.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/dominio/catalogo/Impuesto.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/dominio/catalogo/FormaPago.java`
  - `Desarrollo/sginmo-web/src/main/webapp/monedas.xhtml`
  - `Desarrollo/sginmo-web/src/main/webapp/impuestos.xhtml`
  - `Desarrollo/sginmo-web/src/main/webapp/formas-pago.xhtml`
  - `Desarrollo/sginmo-web/src/main/resources/db/migration/V1__esquema_inicial.sql`
  - `Desarrollo/sginmo-web/src/main/resources/db/migration/V2__seed_basico.sql`
  - `Desarrollo/sginmo-web/src/main/resources/db/migration/V4__version_optimista.sql`
  - `Desarrollo/sginmo-web/src/main/resources/db/migration/V12__abms_catalogo.sql`
  - `docs-migracion/11-estandar-abm-propuesta.md`
- Confirmado correcto:
  - paginacion lazy y orden por whitelist en servicios;
  - `Autorizacion.exigir` en `guardar` y `cambiarEstado`;
  - `@Version` heredado desde `Auditable` y columnas `version` agregadas por V4;
  - `moneda.estado` y `forma_pago.habilitado` en V12;
  - unicidad funcional de `moneda.descripcion` y `forma_pago.codigo`;
  - `por_defecto` de forma de pago se apaga en las demas dentro de la misma transaccion.

## Hallazgos Bloqueantes

### Obs 204 - Impuestos: factores calculados en columnas invertidas

Problema: En modo simplificado, `ImpuestoService.guardar()` calcula los factores al reves respecto al seed y al test plan. Para IVA 10%, `V2__seed_basico.sql` define `factor_discriminado=11` y `factor_impuesto=1.10`; el servicio guarda `factorImpuesto=11` y `factorDiscriminado=1.10`. Para exenta tambien invierte: guarda `factorImpuesto=0` y `factorDiscriminado=1`, mientras el seed exige `factor_discriminado=0` y `factor_impuesto=1.00`.

Impacto: Los impuestos creados o editados desde el ABM quedan incompatibles con los impuestos semilla y con las formulas esperadas por documentos/cobros. Cuando esos motores usen `factor_discriminado`/`factor_impuesto`, pueden discriminar IVA o netos con montos incorrectos.

Solucion esperada: En modo simplificado, guardar:
- porcentaje 10: `factorDiscriminado=11`, `factorImpuesto=1.10`, `porcentajeBaseGravada=100`;
- porcentaje 5: `factorDiscriminado=21`, `factorImpuesto=1.05`, `porcentajeBaseGravada=100`;
- porcentaje 0/exenta: `factorDiscriminado=0`, `factorImpuesto=1.00`, `porcentajeBaseGravada=100`.
Agregar prueba o evidencia reproducible de guardado simplificado.

Evidencia:
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/ImpuestoService.java:73`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/ImpuestoService.java:79`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/ImpuestoService.java:80`
- `Desarrollo/sginmo-web/src/main/resources/db/migration/V2__seed_basico.sql:129`

### Obs 205 - Formas de pago: la REQ exige 14 requisitos configurables, pero hay 13

Problema: El criterio de aceptacion dice "pestana con los 14 requisitos del cobro". La tabla, la entidad y la pantalla exponen solo 13 flags `requiere_*`: emisor, procesador, numero, serie, vencimiento, cuenta, referencia, cobrador, fecha deposito, numero deposito, estado deposito, motivo rechazo y nota credito.

Impacto: Si Oracle/Gestion tiene un requisito adicional, el ABM nuevo no puede configurarlo y el cobro no podra exigir ese dato segun forma de pago. Esto replica una estructura incompleta en una tabla que despues gobierna validaciones operativas.

Solucion esperada: Comparar contra `formas_pago` de Oracle/Gestion, incorporar el requisito faltante si existe (migracion, entidad, UI, seed y validacion de cobro futura) o documentar una decision explicita del usuario corrigiendo el criterio de aceptacion de 14 a 13.

Evidencia:
- `Desarrollo/sginmo-web/src/main/resources/db/migration/V1__esquema_inicial.sql:68`
- `Desarrollo/sginmo-web/src/main/resources/db/migration/V1__esquema_inicial.sql:80`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/dominio/catalogo/FormaPago.java:34`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/dominio/catalogo/FormaPago.java:46`
- `Desarrollo/sginmo-web/src/main/webapp/formas-pago.xhtml`

### Obs 206 - Los tres ABMs no replican el estandar completo aprobado en REQ-0006

Problema: REQ-0008 exige "Estandar completo en los tres". Las pantallas `monedas.xhtml`, `impuestos.xhtml` y `formas-pago.xhtml` tienen paginacion lazy, orden, busqueda global y auditoria por permiso, pero no implementan componentes obligatorios del estandar de referencia: selector de columnas, exportacion CSV/XML/PDF por permiso `EXPORTAR`, busqueda avanzada bajo demanda con limpieza, boton limpiar filtros, vistas/Mi vista cuando aplica, y mensajes vacios diferenciados.

Impacto: Se crean tres ABMs con una experiencia y contrato inferior al ABM estandar que despues se debe replicar. Esto rompe consistencia y obliga a retrabajo en catalogos economicos muy usados.

Solucion esperada: Replicar el contrato de REQ-0006 en los tres ABMs o documentar una excepcion aprobada por el usuario para catalogos simples. Como minimo: column toggler, export pageOnly excluyendo Acciones y protegido por `EXPORTAR`, busqueda avanzada/limpieza donde haya mas de un campo filtrable, mensajes vacios diferenciados y persistencia/vistas segun el patron ya aprobado.

Evidencia:
- `docs-migracion/11-estandar-abm-propuesta.md`
- `.ai-handoff/requirements/REQ-0006/codex-review.md`
- `Desarrollo/sginmo-web/src/main/webapp/monedas.xhtml`
- `Desarrollo/sginmo-web/src/main/webapp/impuestos.xhtml`
- `Desarrollo/sginmo-web/src/main/webapp/formas-pago.xhtml`
- Busqueda `rg "columnToggler|dataExporter|EXPORTAR"` sobre esas tres pantallas no encuentra exportacion ni selector; solo aparecen columnas de auditoria por `VER_AUDITORIA`.

## Pruebas

- Revision estatica completa de servicios, entidades, XHTML y migraciones del REQ.
- No se ejecuto `mvn package` porque hay hallazgos funcionales bloqueantes por inspeccion estatica; el build no cambia el dictamen.

