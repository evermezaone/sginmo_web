# REQ-0083 - Portal cliente: informar transferencia + adjuntar comprobante + bandeja de revision + aplicacion (Fase 1)

**Numero:** REQ-0083
**Fecha de creacion:** 2026-07-13
**Estado inicial:** NUEVO
**Prioridad:** alta

> **Nota de descomposicion:** el pedido original (comprobante + OCR + validacion bancaria + aplicacion) se
> divide en 3 fases entregables e independientes. Ver `analysis.md` para el plan maestro.
> - **REQ-0083 (esta) Fase 1:** informar transferencia en el portal, adjuntar comprobante, bandeja operativa
>   interna y aplicacion del pago (revision/aprobacion MANUAL reutilizando el servicio de cobros). SIN OCR ni
>   validacion bancaria automatica.
> - **REQ-0084 Fase 2:** OCR y extraccion de campos del comprobante.
> - **REQ-0085 Fase 3:** conciliacion bancaria (avisos del banco) y autoaplicacion por umbral.

## Texto Original

"En el portal de cliente el cliente pueda adjuntar comprobante de pago de una transferencia, y que dicho archivo pase por OCR, validacion con mail del banco y luego aplicar el pago a su cuenta corriente." (Descompuesto: esta Fase 1 cubre informar+adjuntar+bandeja+aplicacion manual.)

## Objetivo Funcional

Que un cliente o propietario autenticado en el portal externo (REQ-0078) informe una transferencia bancaria y adjunte el comprobante para pagar cuotas/documentos de su cuenta corriente. El tramite queda en una bandeja operativa interna donde un usuario lo revisa y, si es valido, aplica el pago reutilizando el servicio transaccional de caja/cobros (forma de pago TRANSFERENCIA). Sin OCR ni cruce bancario automatico (esos llegan en 0084/0085); la validacion en esta fase es manual contra la evidencia del operador.

## Alcance Funcional

### Portal (cliente/propietario)
- Opcion "Informar transferencia" en el portal externo.
- Mostrar cuotas/documentos pendientes de la persona y permitir seleccionar cuotas a imputar o "pago a cuenta" (si la config lo permite).
- Adjuntar comprobante en PDF/JPG/PNG/WEBP con validacion de tipo MIME real, extension, tamano maximo y hash SHA-256; guardado fuera del webroot (mismo almacenamiento controlado que documento_adjunto, REQ-0053).
- Capturar campos declarados: banco origen, cuenta origen o ultimos digitos, fecha/hora, importe, moneda, numero de operacion, observacion.
- Ver estado del tramite: RECIBIDO, EN_REVISION, OBSERVADO, RECHAZADO, APLICADO. Ver motivo si fue observado/rechazado.
- Descargar el recibo cuando el pago quede APLICADO.

### Bandeja operativa (pantalla interna)
- Listar transferencias informadas con filtros por estado, cliente, fecha, importe y banco.
- Ver los datos declarados y descargar el archivo original (seguro).
- Aprobar/aplicar, observar o rechazar; observacion/rechazo exigen motivo visible para el cliente.
- Al APROBAR: aplicar el pago con el MISMO servicio transaccional de cobros (no duplicar reglas), imputando a las cuotas seleccionadas o por regla configurada (mas vencido primero / seleccion del cliente / pago a cuenta); forma de pago TRANSFERENCIA con sus datos bancarios; actualiza saldo/cuotas/cuenta corriente/auditoria en una sola transaccion; genera recibo.
- Anti-doble-aplicacion: un numero_transaccion no puede quedar APLICADO dos veces (unico por tenant entre los aplicados).

### Modelo de datos (Fase 1)
- `portal_pago_transferencia`: persona, tenant, sucursal, estado, importe, moneda, fecha_transferencia, banco_origen, cuenta_origen, cuenta_destino, numero_transaccion, observacion_cliente, motivo_revision, cobro (FK al cobro generado, nullable), auditoria. RLS por tenant. (Se dejan nullable los campos de OCR/conciliacion para las fases 2/3.)
- `portal_pago_transferencia_archivo`: referencia al archivo, mime_type, hash_sha256, tamano. (columna texto_ocr nullable, reservada para Fase 2.)

### Configuracion (Fase 1)
- Cuentas receptoras (destino) por empresa, formatos/tamanos permitidos, politica de pago a cuenta, monto maximo informable.

### Seguridad
- El portal solo ve/crea comprobantes de la persona autenticada (persona+tenant, RLS).
- Permisos internos separados: ver / revisar / aprobar-aplicar / rechazar / descargar archivo / configurar.
- Auditoria funcional en cada cambio de estado (carga, observacion, rechazo, aplicacion).

## Criterios De Aceptacion

- [ ] Un cliente autenticado en portal informa una transferencia, adjunta comprobante y lo ve en estado RECIBIDO/EN_REVISION.
- [ ] El archivo se guarda de forma segura con validacion de tipo MIME real, extension, tamano y hash; se descarga solo por su dueno o por la bandeja interna.
- [ ] La bandeja interna lista, filtra, permite ver/descargar el comprobante y aprobar/observar/rechazar con motivo.
- [ ] Al aprobar, el pago se aplica con el servicio transaccional de cobros (forma TRANSFERENCIA), imputa segun seleccion/regla y deja recibo descargable; estado APLICADO.
- [ ] Un mismo numero_transaccion no puede aplicarse dos veces.
- [ ] El cliente solo ve sus propios comprobantes y estados.
- [ ] Auditoria funcional completa de cada cambio de estado.
- [ ] Build `mvn -q clean package` finaliza con EXIT 0.

## Fuera De Alcance (va en 0084/0085)
- OCR y extraccion de campos (REQ-0084).
- Importacion/parseo de avisos del banco, cruce y autoaplicacion por umbral (REQ-0085).

## Dependencias
- Depende de: REQ-0078 (portal externo), REQ-0022/0023 (caja/cobros), REQ-0053 (adjuntos), REQ-0067 (auditoria).
- Requerido por: REQ-0084 y REQ-0085.
