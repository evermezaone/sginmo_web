# REQ-0077 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0077 (BUG)
- Tipo de cambio: UI (operaciones.xhtml). Sin backend, sin BD.
- Riesgo: bajo (quita un required redundante; la validacion queda en el servicio)
- Archivos clave:
  - `webapp/operaciones.xhtml` (frmDetalle, pestana Finalizar/Rescindir): se quito `required="true"` y `requiredMessage` de `txtMotivoResc`. Motivo: los botones de impresion (estado de cuenta/contrato/pagares) son `ajax="false"` -> submit completo del form -> el required del motivo fallaba en CUALQUIER accion, cerraba el dialogo y no ejecutaba la descarga. `OperacionService.finalizar()` ya valida el motivo (NegocioException), asi que la obligatoriedad se conserva solo al finalizar.
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - Deploy + `python tools/smoke-test-vps.py`: 36/36 (sin regresion).
- Cambios de datos: no. Cambios de entorno: no.
- Decision esperada: aprobar; verificar que finalizar sigue exigiendo el motivo (server-side).
- Notas para auditor:
  - La validacion de obligatoriedad se mueve del required de JSF (global al submit del form) al servicio (aplica solo a finalizar). El bean muestra el mensaje en msjDetalle.
  - No se puede usar `process` para acotar los botones de descarga porque son `ajax="false"` (submit completo); por eso el fix correcto es no tener campos `required` en frmDetalle.

## Resumen Funcional

Las acciones/impresiones del detalle de la operacion (estado de cuenta, contrato, pagares) funcionan; ya
no aparece el mensaje del motivo de rescision al usarlas. Finalizar/Rescindir sigue exigiendo el motivo.

## Resumen Tecnico

Se elimino el required de JSF de txtMotivoResc; la obligatoriedad la impone el servicio finalizar().

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| webapp/operaciones.xhtml | quitar required/requiredMessage de txtMotivoResc |

## Cambios De Datos

Sin cambios.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; deploy; smoke 36/36.

## Pruebas Manuales Sugeridas

1. Abrir el detalle de una operacion -> Estado de cuenta / Contrato / Pagares -> descarga el PDF (sin mensaje del motivo).
2. Pestana Finalizar sin motivo -> "El motivo de la finalizacion/rescision es obligatorio" (server-side).
3. Finalizar con motivo -> finaliza y libera el activo.
4. Renovar / regenerar cuotas -> funcionan.

## Riesgos Conocidos

- Bajo. La obligatoriedad del motivo se mantiene (server-side).
