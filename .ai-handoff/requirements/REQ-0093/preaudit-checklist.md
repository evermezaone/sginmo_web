# Preauditoria Claude - REQ-0093

Fecha: 2026-07-14
Responsable: Claude

- [x] Lei observaciones previas aplicables (REQ nuevo).
- [x] Consulte AUDITORIA_OBSERVACION: sin observaciones pendientes.
- [x] Sin observaciones que cerrar (N/A).
- [x] Sin credenciales/tokens/hosts sensibles hardcodeados (la cuenta destino es un parametro, no va en codigo).
- [x] req.md sin criterios [ ] pendientes.
- [x] claude-implementation.md con Manifiesto Minimo, archivos clave y comandos probados.
- [x] test-plan.md solo afirma funcionalidades reales.
- [x] Migracion V59 idempotente (INSERT ... WHERE NOT EXISTS); no rompe si se re-aplica.
- [x] Dependencia nueva (ZXing) documentada; Java puro, sin binario nativo.
- [x] Feature deshabilitada por defecto (no altera el portal hasta configurarla).
- [x] Ejecute handoff:check y paso sin errores.

Notas:

- El pago por QR (Fase 1) es estatico: sin conciliacion automatica (Fase 2 = REQ-0094). El socio informa la transferencia tras pagar.
- El contenido del merchant account template (tag 26) lo define la empresa/banco via parametros.

## Respuesta Por Observacion Cerrada

N/A - REQ nuevo.

```text
Obs 315 (alta, parametros obligatorios EMVCo/SIPAP):
- Problema: habilitado() exigia PORTAL_QR_CUENTA pero no PORTAL_QR_GUI; sin GUI (tag 26 subtag 00) el payload EMVCo/SIPAP queda bancariamente incompleto.
- Cambio: habilitado() ahora exige ademas PORTAL_QR_GUI no vacio; sin GUI el QR no se muestra (queda deshabilitado).
- Archivos: servicio/QrPagoService.java.
- Evidencia: Build OK; smoke 37/37.
- Validacion propia: sin GUI cargado, qr.habilitado()=false -> no se genera el QR (evita QR incompleto).
```

