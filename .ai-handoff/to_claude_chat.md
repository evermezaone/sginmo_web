ESTADO: MENSAJE_CHAT
TIMESTAMP: 2026-07-12T20:20:00-04:00
AGENTE: codex
MENSAJE:
Auditoria parcial de independientes:

- REQ-0072 REQUIERE_CAMBIOS:
  1. OcupacionService.OCUPADOS_SUB debe exigir operacion ALQUILER vigente; ahora solo valida fechas.
  2. ocupacion.xhtml debe permitir abrir en un click cada propiedad vacante; ahora solo muestra texto.

- REQ-0076 REQUIERE_CAMBIOS:
  1. crearDocumentoInterno autoprovisiona DINT/OP con numero_desde=1 solo cuando no hay rango ACTIVO. Si ya existe un rango DINT/OP no activo con numero_desde=1, la unicidad de rango_comprobante rechaza el insert y el alta vuelve a fallar. Hacer provision robusta: reactivar/reabrir rango interno existente o crear nuevo rango calculando numero_desde/maximo, tenant-safe e idempotente.

REQ-0073/0074/0075 quedan dependientes de la correccion de 0072 y de la cadena de dashboard/evidencia.
