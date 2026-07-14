# Preauditoria Claude - REQ-0092

Fecha: 2026-07-14
Responsable: Claude

- [x] Lei observaciones previas aplicables (REQ nuevo).
- [x] Consulte AUDITORIA_OBSERVACION: sin observaciones pendientes.
- [x] Sin observaciones que cerrar (N/A).
- [x] Sin credenciales/tokens/hosts sensibles en los archivos tocados.
- [x] req.md sin criterios [ ] pendientes.
- [x] claude-implementation.md con Manifiesto Minimo, archivos clave y comandos probados.
- [x] test-plan.md solo afirma funcionalidades reales.
- [x] Borrado ATOMICO vs. concurrencia (DELETE...WHERE estado='RECIBIDO' RETURNING) + aislamiento por persona/RLS revisados.
- [x] Sin migracion nueva (reusa tabla/estados de REQ-0083).
- [x] Sin regla general nueva.
- [x] Ejecute handoff:check y paso sin errores.

Notas:

- El borrado se rechaza si la fila ya paso a EN_REVISION (0 filas afectadas). Evidencia fisica borrada best-effort.

## Respuesta Por Observacion Cerrada

N/A - REQ nuevo.

```text
Obs 313 (alta, transicion a EN_REVISION):
- Problema: seleccionar() abria el dialogo pero no reclamaba la fila; el socio podia eliminar una RECIBIDO mientras el operador la revisaba.
- Cambio: nuevo PortalTransferenciaService.reclamar(id) atomico (UPDATE ... WHERE estado=RECIBIDO -> EN_REVISION); TransferenciaBandejaBean.seleccionar() lo llama y recarga la grilla; el boton actualiza frm:tabla. Una vez EN_REVISION, eliminar() del socio ya no aplica.
- Archivos: servicio/PortalTransferenciaService.java, web/TransferenciaBandejaBean.java, webapp/transferencias.xhtml.
- Evidencia: Build OK; smoke 37/37.

Obs 314 (media, acciones sin verificacion):
- Problema: observar()/rechazar() (via cambiarEstado) permitian pasar de RECIBIDO directo a OBSERVADO/RECHAZADO sin pasar por EN_REVISION.
- Cambio: cambiarEstado ahora exige estado IN (EN_REVISION, OBSERVADO); desde RECIBIDO falla con "Primero tome la transferencia para verificar". Como seleccionar() reclama, el flujo real pasa por verificacion.
- Archivos: servicio/PortalTransferenciaService.java.
- Evidencia: Build OK; smoke 37/37.
```

