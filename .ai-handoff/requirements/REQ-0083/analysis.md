# REQ-0083 - Analisis y plan maestro (comprobante de transferencia en el portal)

## Resumen

El pedido original ("adjuntar comprobante + OCR + validacion con mail del banco + aplicar a cuenta corriente")
es una plataforma completa de auto-pago por transferencia. Tiene 3 bloques con dependencias externas de peso
distinto (almacenamiento seguro, motor de OCR, integracion con el banco). Para entregar valor pronto y de forma
segura, se descompone en **3 REQ / fases** independientes y desplegables:

| Fase | REQ | Que entrega | Dependencia externa |
|------|-----|-------------|---------------------|
| 1 | **REQ-0083** | El cliente informa la transferencia + adjunta comprobante; bandeja operativa interna; el operador revisa y **aplica** el pago reutilizando cobros. | Ninguna (usa lo existente) |
| 2 | **REQ-0084** | **OCR** del comprobante: extrae importe/fecha/nro/banco con nivel de confianza; reglas por banco. | Motor OCR (Tesseract local recomendado) |
| 3 | **REQ-0085** | **Conciliacion bancaria**: importa avisos del banco, cruza contra el comprobante y **autoaplica** por umbral; anti-doble-aplicacion. | Casilla/registros del banco |

Cada fase es utilizable por si sola: con la Fase 1 los clientes ya pueden pagar por transferencia (con revision
humana). La 2 reduce el trabajo del operador (datos precargados por OCR). La 3 automatiza la validacion.

## Principios de diseno

- **Reutilizar, no duplicar:** la aplicacion del pago SIEMPRE pasa por el servicio transaccional de caja/cobros
  (`CajaService`/`f_...`), con forma de pago TRANSFERENCIA. El portal/bandeja solo orquesta; el motor de cobro
  es el mismo que caja presencial (regla de negocio unica).
- **Nunca autoaplicar sin evidencia:** en Fase 1 toda aplicacion es aprobacion manual; en Fase 3 solo autoaplica
  con match bancario confirmado + umbral. Sin match/aprobacion, queda en bandeja.
- **Aislamiento:** el portal solo ve/crea comprobantes de la persona autenticada (persona+tenant, RLS), igual que
  el resto del portal (REQ-0078). Archivos fuera del webroot con validacion MIME/tamano/hash (como REQ-0053).
- **Idempotencia / anti-doble-pago:** `numero_transaccion` unico entre aplicados por tenant (Fase 1); en Fase 3
  ademas el `movimiento_bancario_importado` se marca conciliado y no puede reusarse.
- **Auditoria y permisos separados** por accion (ver/revisar/aprobar/rechazar/descargar/configurar), REQ-0067/0064.

## Modelo de datos (incremental)

- **Fase 1:** `portal_pago_transferencia` (+ estados, importe, moneda, fecha, banco/cuenta origen, cuenta destino,
  numero_transaccion, observacion, motivo_revision, cobro FK) y `portal_pago_transferencia_archivo`
  (referencia, mime, hash_sha256, tamano; `texto_ocr` nullable reservado). RLS por tenant.
- **Fase 2:** agrega a las tablas anteriores columnas de OCR (texto, campos extraidos, confianza por campo) o una
  tabla `portal_pago_transferencia_ocr`. Sin romper Fase 1.
- **Fase 3:** `movimiento_bancario_importado` + relacion transferencia<->movimiento; columnas de conciliacion.

## Decisiones abiertas (se resuelven al entrar a cada fase)

- **Fase 2 - motor OCR:** recomendacion **Tesseract** via `tess4j` (local, sin costo, offline) frente a un servicio
  cloud (mejor precision pero costo + envio de datos afuera). Requiere instalar Tesseract en la VPS. **A confirmar
  al iniciar 0084.**
- **Fase 3 - fuente bancaria:** correo IMAP de la casilla del banco vs. importacion de archivo/extracto vs. carga
  manual. Fase 3 arranca con **carga manual + importacion de archivo** (sin credenciales del banco) y deja IMAP
  como extension. **A confirmar al iniciar 0085.**
- **Politica de pago a cuenta** (permitir pago sin imputacion exacta): parametrizable; default OFF.

## Orden sugerido

1. **REQ-0083 (Fase 1)** ahora: es el nucleo, sin bloqueos externos, y ya deja el circuito usable.
2. **REQ-0084 (Fase 2 - OCR)** despues de confirmar el motor.
3. **REQ-0085 (Fase 3 - banco)** al final, con la fuente bancaria definida.
