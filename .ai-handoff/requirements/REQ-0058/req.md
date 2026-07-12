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

- [ ] No se agrega JasperReports ni `.jrxml`.
- [ ] Existe generacion PDF para recibo de cobro con datos reales de cliente, operacion, cuotas, moneda, forma de pago y usuario.
- [ ] Existe generacion PDF para egreso/ingreso manual si el modulo lo soporta.
- [ ] Existe generacion PDF para liquidacion de propietario cuando aplique.
- [ ] Existe generacion PDF para arqueo/cierre de caja cuando aplique.
- [ ] Los comprobantes incluyen numero, fecha/hora, empresa/sucursal, usuario, filtros o referencia de origen.
- [ ] Permite reimprimir comprobantes ya generados conservando trazabilidad.
- [ ] Permisos separados para generar, descargar y reimprimir.
- [ ] La salida respeta formato local de numeros y moneda.

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
