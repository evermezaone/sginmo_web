# REQ-0092 - Portal socio: informar transferencia con evidencia, maquina de estados y seguimiento

**Numero:** REQ-0092
**Fecha de creacion:** 2026-07-14
**Estado inicial:** NUEVO
**Prioridad:** media

## Texto Original

"...la posibilidad de levantar mi evidencia de transferencia bancaria (que queda pendiente de aprobacion y aplicacion)..."

Aclaracion posterior del usuario:
"que la transferencia adjuntada por el usuario quede en estado pendiente de validacion; en este estado
puede eliminarse todavia el registro. Luego pasa a verificacion y alli ya no se puede eliminar, solo se
puede confirmar o rechazar."

## Objetivo Funcional

Que el socio pueda informar una transferencia bancaria adjuntando la evidencia, y que ese registro siga una
maquina de estados clara con reglas de quien puede hacer que en cada estado. El backend base existe
(REQ-0083: PortalTransferenciaService.informar/aprobar/observar/rechazar + portal/transferencia.xhtml).
Este REQ agrega: (1) la fase inicial "pendiente de validacion" con borrado por el socio, (2) el bloqueo del
borrado al pasar a "verificacion", y (3) el seguimiento de estado visible en la cuenta.

## Maquina De Estados

Estado (codigo actual en REQ-0083) -> quien actua -> acciones permitidas:

1. **PENDIENTE_DE_VALIDACION** (hoy 'RECIBIDO') — recien informada por el socio.
   - Socio: puede **ELIMINAR** el registro (borra tambien la evidencia adjunta) mientras siga en este estado.
   - Operador: puede tomarla para revisar -> pasa a VERIFICACION.
2. **VERIFICACION** (hoy 'EN_REVISION') — el operador la reclamo para validar.
   - Socio: **NO puede eliminar** (ni editar).
   - Operador: solo **CONFIRMAR** (aprobar -> aplica el cobro, estado APLICADO) o **RECHAZAR** (RECHAZADO),
     u OBSERVAR (OBSERVADO) pidiendo correccion.
3. **APLICADO / RECHAZADO / OBSERVADO** — estados finales/derivados ya existentes (REQ-0083).

Nota de nomenclatura: se pueden renombrar los codigos a PENDIENTE_DE_VALIDACION / VERIFICACION o mantener
RECIBIDO / EN_REVISION con etiquetas de UI; definir en analisis (preferible etiquetas claras para el socio).

## Alcance

- Agregar **eliminar(transferenciaId, persona)** en PortalTransferenciaService:
  - Permite el borrado SOLO si la transferencia pertenece a la persona (aislamiento) Y esta en estado
    PENDIENTE_DE_VALIDACION.
  - Debe ser **atomico contra el reclamo del operador**: si en el mismo instante pasa a VERIFICACION, el
    borrado falla (UPDATE/DELETE ... WHERE estado = pendiente RETURNING; 0 filas -> no se puede eliminar).
  - Borra tambien el archivo de evidencia del storage y audita el evento.
- UI del portal (integrado en la cuenta / inicio.xhtml, ver REQ-0091):
  - Boton "informar transferencia" (reusa transferencia.xhtml de REQ-0083).
  - Lista "mis transferencias" con estado; boton **Eliminar** visible solo en PENDIENTE_DE_VALIDACION.
  - Descarga de la evidencia; motivo visible si OBSERVADO/RECHAZADO.
- Lado operador (bandeja interna, transferencias.xhtml): en VERIFICACION, acciones Confirmar / Rechazar /
  Observar; el borrado del socio ya no aplica.

## Criterios De Aceptacion

- [x] Al informar, la transferencia queda en PENDIENTE_DE_VALIDACION.
- [x] En PENDIENTE_DE_VALIDACION el socio puede eliminar su registro (se borra tambien la evidencia) y se audita.
- [x] Cuando el operador la toma, pasa a VERIFICACION y el socio ya NO puede eliminarla.
- [x] En VERIFICACION el operador solo puede confirmar o rechazar (u observar); confirmar aplica el cobro.
- [x] El borrado es seguro ante concurrencia (si paso a VERIFICACION, el intento de borrado falla).
- [x] Un socio solo ve/elimina sus propias transferencias (aislamiento por persona + tenant).

## Dependencias

- Base existente: REQ-0083 (PortalTransferenciaService, portal/transferencia.xhtml, tabla portal_pago_transferencia).
- Relacionado con REQ-0091 (panel de pagos) y REQ-0094 (conciliacion automatica).
