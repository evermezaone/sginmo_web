# REQ-0071 - Rentabilidad gerencial, ingresos y egresos por tipo

**Numero:** REQ-0071
**Fecha de creacion:** 2026-07-12
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

Del usuario (2026-07-12): "RENTABILIDAD, egresos... por tipos de ingresos."

## Objetivo Funcional

Agregar al dashboard indicadores de rentabilidad e ingresos/egresos para que gerencia pueda ver
no solo cuanto se cobra, sino cuanto queda, de donde viene y que lo esta consumiendo.

## Criterios De Aceptacion

- [ ] Se calcula ingreso bruto por periodo y tipo: alquiler, venta, comision, mora/interes, deposito/garantia y otros.
- [ ] Se calcula egreso por periodo y tipo/articulo: comisiones pagadas, mantenimiento, impuestos, devoluciones, gastos de liquidacion y otros.
- [ ] Se calcula resultado neto: ingresos - egresos.
- [ ] Se calcula margen porcentual cuando exista base valida.
- [ ] Se muestra rentabilidad por propiedad/activo, tipo de activo, sucursal/zona y propietario cuando la informacion exista.
- [ ] Se incluye ranking de mejores y peores activos por rentabilidad neta.
- [ ] Cada monto permite abrir evidencia: cobros, ingresos/egresos, liquidaciones o documentos que componen el valor.
- [ ] No se mezclan monedas; el usuario debe filtrar por moneda o ver totales separados por moneda.
- [ ] Las reglas de clasificacion dependen de `articulo.aplicacion`, tipo de documento, tipo de operacion o configuracion equivalente, no de textos hardcodeados.
- [ ] Los egresos asociados a una operacion/activo/persona deben conservar trazabilidad.

## Reglas De Negocio

- La rentabilidad debe poder calcularse en base caja (cobrado/pagado) y dejar preparada la extension a base devengada si se requiere despues.
- Las garantias/depositos no deben confundirse con rentabilidad si son pasivos/devoluciones; deben clasificarse explicitamente.
- Las comisiones pueden ser ingreso o egreso segun el rol de la inmobiliaria en la operacion.

## Dependencias

- Depende de: REQ-0018, REQ-0022, REQ-0025, REQ-0069.
- Requerido por: REQ-0070, REQ-0074, REQ-0075.

## Fuentes Y Trazabilidad

- Pedido directo del usuario sobre rentabilidad/egresos.
- Tablas esperadas: `cobro`, `ingreso_egreso`, `operacion`, `activo`, `liquidacion`.
