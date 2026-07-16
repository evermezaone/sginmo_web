# REQ-0092 - Auditoria Codex

**Estado:** APROBADO
**Fecha:** 2026-07-14
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Verificacion

- Al informar, `PortalTransferenciaService.informar()` inserta la transferencia en `RECIBIDO`, mostrado al socio como "Pendiente de validacion".
- El portal muestra transferencias propias con estado, numero, motivo, descarga de evidencia y boton eliminar solo si `estado == RECIBIDO`.
- `PortalTransferenciaService.eliminar()` es atomico: borra solo con `persona = :p` y `estado = 'RECIBIDO'`, usando `DELETE ... RETURNING`.
- La descarga de evidencia valida persona propia para socio o permiso interno para operador.
- La bandeja interna llama `servicio.reclamar(id)` al seleccionar la transferencia, haciendo `RECIBIDO -> EN_REVISION` antes de revisar y bloqueando la eliminacion del socio.
- `observar()` y `rechazar()` ya no saltan directo desde `RECIBIDO`: `cambiarEstado()` solo permite `EN_REVISION` u `OBSERVADO`.
- `aprobar()` mantiene reclamo/lock atomico y aplica el cobro dentro de la transaccion.
- El backend valida firma real del archivo adjunto para PDF/JPG/PNG/WEBP.

## Pruebas Revisadas

- [x] Revision estatica de `PortalTransferenciaService.informar()`.
- [x] Revision estatica de `PortalTransferenciaService.reclamar()`.
- [x] Revision estatica de `PortalTransferenciaService.eliminar()`.
- [x] Revision estatica de `PortalTransferenciaService.aprobar()/observar()/rechazar()`.
- [x] Revision estatica de `PortalTransferenciaBean`.
- [x] Revision estatica de `TransferenciaBandejaBean`.
- [x] Revision estatica de `portal/transferencia.xhtml`.
- [x] Revision estatica de `transferencias.xhtml`.
- [x] Revision estatica de `V56__portal_pago_transferencia.sql`.
- [x] `mvn -q -f Desarrollo/pom.xml -pl sginmo-web -am clean package` EXIT 0.

## Riesgo Residual

- Falta prueba manual real con dos sesiones (socio y operador) para validar la concurrencia en navegador, pero el bloqueo atomico esta implementado en backend.
