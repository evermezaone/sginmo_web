# REQ-0085 - Portal transferencias Fase 3: conciliacion bancaria y autoaplicacion por umbral

**Numero:** REQ-0085
**Fecha de creacion:** 2026-07-13
**Estado inicial:** NUEVO
**Prioridad:** media

> Fase 3 (final) de la plataforma de auto-pago por transferencia (plan maestro en REQ-0083/analysis.md).
> Depende de REQ-0083 (Fase 1) y se apoya en los campos extraidos por REQ-0084 (Fase 2).

## Objetivo Funcional

Incorporar la validacion bancaria: importar/registrar los avisos del banco, cruzarlos automaticamente contra el
comprobante informado y autoaplicar el pago SOLO cuando hay match confirmado y se cumplen los umbrales; en caso
contrario deja el tramite en la bandeja para revision manual (Fase 1). Evita doble aplicacion.

## Alcance Funcional

- Fuentes de avisos bancarios (configurable): carga manual + importacion de archivo/extracto (fase inicial),
  y correo IMAP/API como extension.
- Normalizar los avisos en `movimiento_bancario_importado` (fuente, banco, cuenta, fecha, importe, moneda,
  referencia, remitente, estado de conciliacion, hash/id externo idempotente).
- Cruce comprobante<->movimiento por importe, moneda, fecha (con tolerancia), cuenta destino, numero de
  transaccion/referencia, banco y posible remitente/persona.
- Autoaplicacion cuando: match confirmado + confianza >= umbral + importe <= monto maximo autoaplicable +
  dentro de la tolerancia de fecha. Aplica con el servicio de cobros (igual que Fase 1). Si no cumple, a bandeja.
- Nunca aplicar sin correo/aviso bancario confirmado.
- Anti-doble-aplicacion: un movimiento bancario o numero de transaccion no puede imputarse dos veces (se marca
  conciliado/aplicado y no se reutiliza).
- En la bandeja: mostrar los movimientos bancarios candidatos por transferencia y permitir confirmar/elegir.

## Modelo De Datos

- `movimiento_bancario_importado` + tabla/columna de relacion transferencia<->movimiento (candidato/confirmado).

## Configuracion

- Fuente de correos/avisos, umbral de confianza para autoaplicacion, monto maximo autoaplicable, tolerancia de
  fecha, cuentas receptoras por banco.

## Decision Abierta (confirmar al iniciar)

- **Fuente bancaria:** arrancar con carga manual + importacion de archivo (sin credenciales del banco); IMAP/API
  como extension posterior si el banco provee acceso.

## Criterios De Aceptacion

- [ ] Se importan/registran avisos bancarios y se normalizan en `movimiento_bancario_importado` (idempotente).
- [ ] El sistema cruza comprobantes contra movimientos y sugiere candidatos.
- [ ] No se aplica automaticamente ningun pago sin match bancario confirmado.
- [ ] Con match confiable + umbrales, se autoaplica con el servicio de cobros y queda recibo.
- [ ] Un mismo movimiento/numero de transaccion no se aplica dos veces.
- [ ] Lo dudoso queda en bandeja con trazabilidad.
- [ ] Build `mvn -q clean package` EXIT 0.

## Dependencias

- Depende de: REQ-0083 (Fase 1) y REQ-0084 (Fase 2).
