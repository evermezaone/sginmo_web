# Implementacion Claude - REQ-0008

## Manifiesto Minimo Para Codex
- Moneda/FormaPago (entidades nuevas), Impuesto (existente), servicios Moneda/Impuesto/
  FormaPago con exigir() de permisos, beans y pantallas monedas/impuestos/formas-pago.xhtml.
- V12 (APLICADA en VPS): moneda.estado, forma_pago.habilitado, PANTALLAS de los catalogos.
- Regla especial: FormaPagoService apaga por_defecto en las demas dentro de la misma
  transaccion antes del merge.
- Comandos probados: build+deploy; grillas con datos reales del seed V2.
