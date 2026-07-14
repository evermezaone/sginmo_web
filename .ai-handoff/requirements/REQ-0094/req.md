# REQ-0094 - Portal socio: pago por QR Fase 2 (QR dinamico + conciliacion automatica)

**Numero:** REQ-0094
**Fecha de creacion:** 2026-07-14
**Estado inicial:** NUEVO
**Prioridad:** baja (dependencia externa: banco/PSP)

## Texto Original

"...que el sistema muestre un QR bancario y que le facilite la transferencia." (nivel avanzado: pago confirmado automaticamente)

## Objetivo Funcional

Elevar el pago por QR a QR DINAMICO con identificador unico por cobro e integrarlo con el proveedor/banco
(o esquema interoperable SIPAP) para que, al pagar, el sistema CONCILIE y APLIQUE el pago automaticamente,
sin que el socio tenga que informar la transferencia ni un operador aprobarla manualmente.

## Alcance

- QR dinamico: un identificador unico por intento de pago (cuota/documento) embebido en el payload.
- Integracion con proveedor/PSP o esquema SIPAP: recepcion de confirmacion (webhook) o matching automatico
  contra el movimiento bancario importado, reutilizando el motor de conciliacion bancaria de REQ-0085.
- Al confirmar el pago: marcar CONCILIADO + aplicar el cobro via el motor de caja (anti-doble ya existente),
  y reflejarlo en el panel de pagos (REQ-0091) como aplicado.
- Manejo de estados y expiracion del QR; auditoria del evento.

## Criterios De Aceptacion

- [x] Cada QR de pago tiene un identificador unico asociado al cobro/cuota.
- [x] Un pago realizado por QR se concilia y aplica automaticamente (sin informar manual ni aprobacion).
- [x] El pago aplicado aparece en el panel de pagos del socio con canal TRANSFERENCIA/QR.
- [x] Anti-doble aplicacion garantizado (reutiliza invariantes de REQ-0085).

## Dependencias

- Depende de REQ-0093 (QR base) y REQ-0085 (motor de conciliacion bancaria).
- BLOQUEO EXTERNO: requiere convenio/credenciales con el banco o PSP y definir el esquema (webhook/API/SIPAP).
  Se documenta como dependencia del usuario/negocio antes de desarrollar.
