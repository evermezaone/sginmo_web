# REQ-0085 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-13
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

1. **La conciliacion no valida en backend que el movimiento bancario sea candidato real de la transferencia.**

   `PortalTransferenciaService#conciliarYAplicar()` solo exige que el movimiento este `PENDIENTE`, lo marca `CONCILIADO` con la transferencia recibida y llama a `aprobar()`. No valida en backend que el movimiento pertenezca al mismo tenant por regla explicita, ni que coincida con importe, fecha/tolerancia, referencia/numero, moneda/cuenta/banco segun los criterios del REQ. La UI muestra candidatos, pero el backend permite conciliar cualquier `movimiento_bancario_importado` pendiente que llegue por parametro.

   **Impacto:** un error de UI, manipulacion de request o bug operativo puede confirmar un movimiento bancario que no corresponde al comprobante y aun asi aplicar el cobro. Para pagos, no basta con filtrar en pantalla.

   **Evidencia:** `PortalTransferenciaService.java` lineas 368-376.

   **Solucion esperada:** antes de marcar `CONCILIADO` y aplicar, validar atomicamente que el movimiento cumple los mismos criterios de `candidatos()` para esa transferencia: estado `PENDIENTE`, importe, tolerancia de fecha, referencia/numero si existe, y los campos que correspondan al alcance (moneda/cuenta/banco cuando esten disponibles). Ideal: `UPDATE ... FROM portal_pago_transferencia ... WHERE movimiento=:m AND transferencia=:tr AND estado_conciliacion='PENDIENTE' AND criterios_match RETURNING ...`; si no retorna fila, no aplicar.

2. **La importacion CSV esta dentro de un formulario JSF anidado, por lo que el upload no es confiable.**

   `transferencias.xhtml` tiene un `<h:form id="frm">` que envuelve toda la pagina, y dentro del dialogo `Movimientos bancarios` agrega otro `<h:form enctype="multipart/form-data">` para el CSV. HTML no permite formularios anidados y JSF/PrimeFaces no garantizan que el `p:fileUpload mode="simple"` envie correctamente el archivo en esa estructura. El criterio de aceptacion exige importar CSV.

   **Impacto:** la carga/importacion de avisos bancarios puede fallar o comportarse de forma dependiente del navegador, dejando inutilizable una parte central del REQ.

   **Evidencia:** `transferencias.xhtml` lineas 12 y 56-60.

   **Solucion esperada:** eliminar el formulario anidado. Usar un unico formulario multipart para la pantalla/dialogo, o mover el dialogo de importacion a un formulario separado no anidado fuera de `frm`. Asegurar que el update apunte a componentes validos tras importar.

### No Bloqueantes

- `importarCsv()` incrementa el contador aun cuando `ON CONFLICT DO NOTHING` no inserta fila; el mensaje puede contar lineas procesadas, no movimientos realmente insertados. Es cosmetico/operativo, no bloquea si se corrigen los dos puntos anteriores.

## Validacion Realizada

- Revision estatica de `PortalTransferenciaService` metodos `registrarMovimiento`, `importarCsv`, `candidatos` y `conciliarYAplicar`.
- Revision estatica de `TransferenciaBandejaBean`.
- Revision estatica de `transferencias.xhtml`.
- Revision estatica de `V58__movimiento_bancario.sql`.

## Pruebas

- No se aprobo la entrega por hallazgos funcionales/de seguridad. El build no detecta estos dos problemas.
