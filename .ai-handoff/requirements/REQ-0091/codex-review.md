# REQ-0091 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-14
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

1. El panel lateral "Mis pagos" no muestra el estado de cada pago, aunque el criterio de aceptacion exige: "Cada pago indica claramente el canal (CAJA / TRANSFERENCIA) y su estado". `PortalService.FilaPago` trae `estado`, pero `portal/inicio.xhtml` solo renderiza monto, canal, fecha y forma. Debe mostrarse el estado con etiqueta/badge legible junto al canal o en la linea secundaria.

2. El alcance pide exponer moneda y referencia/numero de comprobante si existe. `PortalService.pagos()` solo selecciona fecha, monto, estado, forma/canal y concepto; no devuelve moneda ni referencia/numero de comprobante. Si se decide no incluirlos, debe quedar fundado como recorte de alcance; si no, debe implementarse.

### No Bloqueantes

- El filtro de seguridad principal se mantiene: `PortalService.pagos()` filtra por `c.persona = :p` y el service conserva `@AislarTenant`.
- El layout de dos columnas y el stacking responsive estan implementados en `portal.xhtml`.

## Riesgos

- El usuario ve el canal del pago, pero no puede distinguir visualmente su estado desde el nuevo panel, perdiendo parte del seguimiento pedido.

## Pruebas Revisadas

- [x] Revision estatica de `PortalService.pagos()` y `FilaPago`.
- [x] Revision estatica de `PortalBean`.
- [x] Revision estatica de `portal/inicio.xhtml`.
- [x] Revision estatica de CSS responsive en `WEB-INF/portal.xhtml`.
- [x] `mvn -q -pl sginmo-web -am clean package` EXIT 0.

## Pruebas Faltantes

- [ ] Prueba manual de portal con CI/RUC + OTP para validar vista real desktop/mobile.
