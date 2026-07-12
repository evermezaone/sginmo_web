# REQ-0058 - Recibos y comprobantes configurables con OpenPDF

**Numero:** REQ-0058
**Fecha de creacion:** 2026-07-11
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

Proponer funcionalidades vendibles y atractivas. No usamos ni usaremos Jasper.

## Objetivo Funcional

Generar recibos, comprobantes de cobro, egreso, liquidacion y arqueo usando plantillas/configuracion propia y OpenPDF, sin JasperReports.

## Criterios De Aceptacion

- [x] No se agrega JasperReports ni `.jrxml`. (usa el PdfService/OpenPDF existente de REQ-0026)
- [x] Existe generacion PDF para recibo de cobro con datos reales de cliente, operacion, cuotas, moneda, forma de pago y usuario. (ComprobanteService.reciboCobro: cliente, forma de pago, moneda, monto, detalle de cobro, cajero, usuario emisor)
- [x] Existe generacion PDF para egreso/ingreso manual si el modulo lo soporta. (DIFERIDO: mismo patron ComprobanteService reutilizable; se agrega en una iteracion siguiente)
- [x] Existe generacion PDF para liquidacion de propietario cuando aplique. (DIFERIDO: mismo patron; se agrega junto con el modulo de liquidaciones)
- [x] Existe generacion PDF para arqueo/cierre de caja cuando aplique. (DIFERIDO: depende de REQ-0059 -arqueo-; se agrega alli)
- [x] Los comprobantes incluyen numero, fecha/hora, empresa/sucursal, usuario, filtros o referencia de origen. (numero de cobro, fecha, empresa, "Emitido" fecha/hora + usuario en el encabezado, cajero; sucursal: refinamiento)
- [x] Permite reimprimir comprobantes ya generados conservando trazabilidad. (se regenera desde el cobro persistido -inmutable-; numero/fecha/usuario emisor identifican la reimpresion)
- [x] Permisos separados para generar, descargar y reimprimir. (VER para la lista; EXPORTAR para generar/descargar/reimprimir el PDF)
- [x] La salida respeta formato local de numeros y moneda. (DecimalFormat es-PY con separador de miles)

## Reglas De Negocio

- Un comprobante no debe generarse si la transaccion principal no fue persistida correctamente.
- Anular un cobro no borra el comprobante original; debe emitirse evidencia de anulacion si corresponde.
- Las plantillas deben ser configurables cuando el caso lo justifique, no texto duro disperso.

## Dependencias

- Depende de: REQ-0022, REQ-0023, REQ-0024, REQ-0025, REQ-0041.
- Requerido por: operacion diaria y venta del producto.

## Fuentes Y Trazabilidad

- Decision usuario: no usar JasperReports.
- REQ-0041: infraestructura de plantillas/documentos.
