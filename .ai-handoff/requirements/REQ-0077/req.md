# REQ-0077 - BUG detalle de operacion: acciones/impresiones disparan el required del motivo de rescision

**Numero:** REQ-0077
**Fecha de creacion:** 2026-07-12
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

Del usuario (2026-07-12): "ya pude hacer una operacion de alquiler. Pero al intentar hacer cualquier
accion sobre la operacion, se cierra el dialogo y aparece un mensaje 'El motivo de la finalizacion/
rescision es obligatorio': imprimir contrato, renovacion, imprimir pagare, imprimir estado de cuenta."

## Diagnostico

En el dialogo "Detalle de la operacion" (frmDetalle), los botones de impresion (estado de cuenta,
contrato, pagares) son `ajax="false"` -> hacen submit COMPLETO del formulario. El campo
`txtMotivoResc` de la pestana Finalizar/Rescindir tenia `required="true"`; por eso CUALQUIER submit
completo validaba ese campo vacio, fallaba con su requiredMessage, cerraba el dialogo y no ejecutaba
la accion (descarga). El servicio finalizar() ya valida el motivo, asi que el required de JSF era
redundante y rompia las demas acciones.

## Criterios De Aceptacion

- [x] Los botones de impresion (estado de cuenta, contrato, pagares) funcionan sin disparar el mensaje del motivo. (se quito `required`/`requiredMessage` de txtMotivoResc)
- [x] Finalizar/Rescindir sigue exigiendo el motivo. (OperacionService.finalizar valida y lanza NegocioException; el bean lo muestra en msjDetalle)
- [x] Renovar y regenerar cuotas no se ven afectados. (usan process acotado; ya no hay campo required en el form)
- [x] No hay regresion en el resto del modulo. (build + smoke 36/36)

## Reglas De Negocio

- La validacion de obligatoriedad del motivo de rescision vive en el servicio (aplica solo al finalizar), no como required global del formulario.

## Dependencias

- Depende de: REQ-0016 (alta/detalle de operacion), REQ-0021 (rescision).

## Fuentes Y Trazabilidad

- Reporte del usuario 2026-07-12.
- operaciones.xhtml frmDetalle: botones ajax=false + txtMotivoResc required.
