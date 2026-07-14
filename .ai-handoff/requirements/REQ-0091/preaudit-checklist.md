# Preauditoria Claude - REQ-0091

Fecha: 2026-07-14
Responsable: Claude

- [x] Lei observaciones previas aplicables (REQ nuevo).
- [x] Consulte AUDITORIA_OBSERVACION: sin observaciones pendientes.
- [x] Sin observaciones que cerrar (N/A).
- [x] Sin credenciales/tokens/hosts sensibles en los archivos tocados.
- [x] req.md sin criterios [ ] pendientes.
- [x] claude-implementation.md con Manifiesto Minimo, archivos clave y comandos probados.
- [x] test-plan.md solo afirma funcionalidades reales.
- [x] Revise aislamiento por persona+tenant en la query de pagos (cobro.persona=:p, @AislarTenant/RLS).
- [x] Sin BD/triggers/SPs tocados.
- [x] Sin regla general nueva.
- [x] Ejecute handoff:check y paso sin errores.

Notas:

- El canal se deriva de forma_pago.codigo. La vista del portal no entra en el smoke (OTP); se valido XML bien formado + getters.

## Respuesta Por Observacion Cerrada

```text
Obs 310 (alta, criterio_aceptacion):
- Problema original: el panel de pagos no mostraba el estado de cada pago (solo monto/canal/fecha/forma).
- Cambio aplicado: se agrego el estado (FilaPago.getEstadoLabel: ACTIVO->Confirmado) al pago-sub, junto con moneda y nro de recibo.
- Archivos tocados: webapp/portal/inicio.xhtml, servicio/PortalService.java (getEstadoLabel).
- Evidencia: XML bien formado; Build OK; smoke 37/37.
- Validacion propia: el panel ahora indica canal Y estado por pago (cumple el criterio).

Obs 311 (media, alcance_incompleto):
- Problema original: pagos() no exponia moneda ni referencia/nro de comprobante.
- Cambio aplicado: pagos() ahora hace LEFT JOIN moneda (simbolo) y LEFT JOIN documento (recibo_documento -> serie-numero); FilaPago.moneda/comprobante + getters; el panel muestra el simbolo de moneda y "Recibo serie-numero" si existe.
- Archivos tocados: servicio/PortalService.java, webapp/portal/inicio.xhtml.
- Evidencia: Build OK; smoke 37/37.
- Validacion propia: alcance completado (moneda + nro de comprobante cuando existe).
```

```text
Obs 312 (media, ronda 2, Portal pagos - referencia/comprobante):
- Problema original: el panel no mostraba la referencia/nro de transaccion bancaria de los pagos por transferencia (guardada en dato_cobro.referencia); pagos() solo miraba documento.recibo_documento.
- Cambio aplicado: pagos() agrega una subconsulta escalar a dato_cobro (referencia no vacia, LIMIT 1) sin duplicar filas; FilaPago.referencia + getter; el panel muestra "Ref {referencia}" cuando existe (ademas del recibo).
- Archivos tocados: servicio/PortalService.java, webapp/portal/inicio.xhtml.
- Evidencia: XML bien formado; Build OK; smoke 37/37.
- Validacion propia: los pagos por transferencia con numero_transaccion ahora muestran la referencia bancaria; los de caja/sin dato quedan sin Ref (limpio).
```

