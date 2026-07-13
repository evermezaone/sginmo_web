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

## Decision Tomada (fuente)

- **Carga manual + importacion de archivo CSV** (sin credenciales del banco), confirmado por el usuario. IMAP/API
  queda como extension posterior (la columna `fuente` ya admite 'IMAP').

## Criterios De Aceptacion

- [x] Se registran (manual) e importan (CSV) avisos bancarios en `movimiento_bancario_importado`, idempotente por hash externo.
- [x] En la bandeja, para una transferencia se muestran los movimientos candidatos (mismo importe + fecha con tolerancia + referencia si coincide).
- [x] El pago se aplica al conciliar con un movimiento confirmado (o "Aprobar sin conciliar" como fallback manual explicito).
- [x] Un mismo movimiento no se concilia dos veces (queda CONCILIADO, no reutilizable) y un numero_transaccion no se aplica dos veces (unique parcial de Fase 1).
- [x] Lo no conciliado queda en la bandeja con su estado; auditoria por cambio de estado.
- [x] Build `mvn -q clean package` EXIT 0; Flyway V58; smoke 37/37.

## Nota de alcance (autoaplicacion)

- La aplicacion SIEMPRE pasa por el motor de caja (requiere una caja abierta), por eso la "autoaplicacion por
  umbral" es asistida por el operador (concilia+aplica desde la bandeja con la caja abierta), no un job desatendido.
  Los parametros PORTAL_TRANSF_MONTO_MAX_AUTO / TOLERANCIA_DIAS quedan cargados para afinar el cruce/limites.

## Dependencias

- Depende de: REQ-0083 (Fase 1) y REQ-0084 (Fase 2).

## Dependencias

- Depende de: REQ-0083 (Fase 1) y REQ-0084 (Fase 2).
