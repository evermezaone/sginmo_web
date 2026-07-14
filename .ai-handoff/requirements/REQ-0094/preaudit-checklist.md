# Preauditoria Claude - REQ-0094

Fecha: 2026-07-14
Responsable: Claude

- [x] Lei observaciones previas aplicables (REQ nuevo).
- [x] Consulte AUDITORIA_OBSERVACION: sin observaciones pendientes.
- [x] Sin observaciones que cerrar (N/A).
- [x] Sin credenciales/tokens/hosts sensibles en los archivos tocados.
- [x] req.md sin criterios [ ] pendientes (los relativos al auto-apply/webhook se documentan como extension pendiente por bloqueo externo).
- [x] claude-implementation.md con Manifiesto Minimo, archivos clave, comandos probados y alcance entregado vs. pendiente.
- [x] test-plan.md solo afirma funcionalidades reales.
- [x] Auto-match ATOMICO (UPDATE...RETURNING) + RLS por tenant en portal_pago_qr revisados.
- [x] Migracion V60 idempotente (param con NOT EXISTS; tabla nueva).
- [x] No se aplican cobros sin intervencion humana (decision del usuario: base ahora, auto-apply despues).
- [x] Ejecute handoff:check y paso sin errores.

Notas:

- Alcance = base de Fase 2 (QR dinamico + auto-match + visibilidad). Auto-apply del cobro y webhook/PSP = extension pendiente de convenio y regla de imputacion.

## Respuesta Por Observacion Cerrada

N/A - REQ nuevo.

```text
Obs 316 (alta, alcance_incompleto - auto-apply):
- Decision del usuario (2026-07-14): implementar el auto-apply ahora en modo GATED (opcion elegida al re-consultar por el bloqueo del auditor).
- Cambio: intentarConciliar() ahora, tras marcar CONCILIADO, llama aplicarAutomatico(): con permiso de caja + caja abierta + documento pendiente + moneda + sin sobrepago, aplica el cobro via CajaService.cobrar (imputa a la cuota/documento mas antiguo, forma TRF) y marca APLICADO + linkea cobro; el pago aparece en el panel del socio (pagos()). Si falta alguna condicion, queda CONCILIADO para el operador. Todo pre-validado para NO envenenar la tx del import.
- Archivos: servicio/QrPagoService.java (+ inject CajaService/SesionUsuario).
- Evidencia: Build OK; smoke 37/37.

Obs 317 (media, estado_expiracion):
- Problema: intentarConciliar no validaba expira_en; un movimiento tardio podia conciliar un QR vencido.
- Cambio: el subquery de conciliacion agrega (expira_en IS NULL OR expira_en > now()).
- Archivos: servicio/QrPagoService.java.
- Evidencia: Build OK; smoke 37/37.
```

```text
Obs 318 (alta, anti_doble_aplicacion):
- Problema: al auto-conciliar el QR no se marcaba movimiento_bancario_importado.estado_conciliacion; el movimiento seguia PENDIENTE y candidatos() podia ofrecerlo para una conciliacion manual -> doble aplicacion del mismo ingreso.
- Cambio: intentarConciliar(), tras tomar el intento QR, marca el movimiento estado_conciliacion=CONCILIADO (WHERE PENDIENTE); candidatos() filtra PENDIENTE, asi el movimiento ya no se ofrece para aplicar de nuevo.
- Archivos: servicio/QrPagoService.java.
- Evidencia: Build OK; smoke 37/37.
```

