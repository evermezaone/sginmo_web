# REQ-0076 - BUG alta de operacion: falla silenciosa y timbrado interno DINT faltante

**Numero:** REQ-0076
**Fecha de creacion:** 2026-07-12
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

Del usuario (2026-07-12): "cargue todos los datos, presione el boton registrar Operacion y no paso nada,
no dio mensaje, ni cerro pantalla."

## Diagnostico

- La BD lanza RAISE 'No hay timbrado ACTIVO para el tipo DINT serie OP' al crear el documento interno
  (DINT/OP) que respalda el cronograma. La empresa no tenia un rango_comprobante interno para DINT/OP.
- ErroresBd.traducir solo traducia constraints; el RAISE (SQLState P0001) se re-lanzaba y llegaba como
  500 al ajax -> la pantalla no mostraba nada.
- Al reintentar (entidad ya persistida por el flush previo al rollback) saltaba "Detached entity passed to persist".

## Criterios De Aceptacion

- [x] El alta de operacion ya no falla por "No hay timbrado ACTIVO para DINT/OP". (crearDocumentoInterno autoprovisiona un rango INTERNO amplio DINT/OP por tenant si falta -documento interno no fiscal-; idempotente, aislado por tenant)
- [x] Los mensajes RAISE de la BD (P0001) se muestran como mensaje de negocio. (ErroresBd surface del mensaje limpio a una linea)
- [x] Ningun fallo del alta queda silencioso. (OperacionBean.crear captura RuntimeException ademas de NegocioException; el boton actualiza msjEdicion)
- [x] El reintento tras un fallo no rompe silenciosamente. (resuelta la causa raiz el alta funciona al primer intento; y cualquier otro error se muestra pidiendo reabrir el dialogo)
- [x] La correccion respeta tenant/RLS. (el rango se inserta con tenant = empresa actual; RLS lo permite)

## Reglas De Negocio

- El documento interno DINT es de uso interno (cuenta corriente del cronograma), no fiscal; no requiere timbrado real.
- Nunca se muestra un error tecnico crudo; los RAISE del motor son mensajes de negocio deliberados.

## Dependencias

- Depende de: REQ-0016 (alta de operacion), REQ-0033 (multiempresa).

## Fuentes Y Trazabilidad

- Reporte del usuario 2026-07-12 (dialogo "Nueva operacion").
- Log WildFly: "No hay timbrado ACTIVO para el tipo DINT serie OP" + "Detached entity passed to persist".
