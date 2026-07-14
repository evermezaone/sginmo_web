# REQ-0092 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-14
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

1. Falta la transicion real a `EN_REVISION` cuando el operador toma la transferencia para verificar. En la bandeja interna, `TransferenciaBandejaBean.seleccionar()` solo carga documentos/candidatos y abre el dialogo; no llama a ningun metodo que reclame la transferencia ni actualiza el estado. Por eso una transferencia `RECIBIDO` sigue siendo eliminable por el socio mientras el operador ya la esta revisando. El REQ exige: "Cuando el operador la toma, pasa a VERIFICACION y el socio ya NO puede eliminarla". Debe existir una accion backend atomica de reclamo/toma para pasar de `RECIBIDO` a `EN_REVISION` antes de revisar, y la UI debe refrescar ese estado.

2. Las acciones de operador `observar()` y `rechazar()` pueden cambiar directamente una transferencia desde `RECIBIDO` a `OBSERVADO`/`RECHAZADO` mediante `cambiarEstado()`, sin pasar por `EN_REVISION`. El flujo requerido dice que en `VERIFICACION` el operador confirma/rechaza/observa. Debe cerrarse la brecha de estados: o se reclama a `EN_REVISION` al seleccionar, o esas acciones deben reclamar/validar primero la transicion.

### Correcto

- Al informar, la transferencia se inserta con estado `RECIBIDO`, usado como etiqueta "Pendiente de validacion".
- El portal muestra lista de transferencias propias con estado, motivo visible y descarga de evidencia.
- El boton eliminar del socio se renderiza solo cuando `t.puedeEliminar`, que depende de `estado == RECIBIDO`.
- `PortalTransferenciaService.eliminar()` borra de forma atomica solo si `persona = :p` y `estado = 'RECIBIDO'`.
- La descarga de comprobante valida que sea propia para el socio, o exige permiso interno si no lo es.
- El backend valida firma real del archivo adjunto para PDF/JPG/PNG/WEBP.

## Riesgos

- La ventana entre "operador abre revision" y "operador confirma/rechaza/observa" permite que el socio elimine el registro, justo lo que el REQ queria evitar.

## Pruebas Revisadas

- [x] Revision estatica de `PortalTransferenciaService.informar()`.
- [x] Revision estatica de `PortalTransferenciaService.eliminar()`.
- [x] Revision estatica de `PortalTransferenciaService.aprobar()/observar()/rechazar()`.
- [x] Revision estatica de `PortalTransferenciaBean`.
- [x] Revision estatica de `TransferenciaBandejaBean`.
- [x] Revision estatica de `portal/transferencia.xhtml`.
- [x] Revision estatica de `transferencias.xhtml`.
- [x] Revision estatica de `V56__portal_pago_transferencia.sql`.
- [x] `mvn -q -f Desarrollo/pom.xml -pl sginmo-web -am clean package` EXIT 0.

## Pruebas Faltantes

- [ ] Prueba manual de concurrencia: socio intenta eliminar mientras operador toma/revisa.
