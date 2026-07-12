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

- [x] Se calcula ingreso bruto por periodo y tipo: alquiler, venta, comision, mora/interes, deposito/garantia y otros. (`porTipo('INGRESO')` agrupa ingreso_egreso por `articulo.aplicacion`; etiquetas legibles)
- [x] Se calcula egreso por periodo y tipo/articulo. (`porTipo('EGRESO')` idem)
- [x] Se calcula resultado neto: ingresos - egresos. (neto = totalIngresos operativos - totalEgresos)
- [x] Se calcula margen porcentual cuando exista base valida. (margenPct = neto/totalIngresos*100 si ingresos>0; si no, 0)
- [x] Se muestra rentabilidad por propiedad/activo (+ tipo/zona/propietario como refinamiento). (rankingActivos por neto; tipo/zona/propietario documentados como incremental)
- [x] Se incluye ranking de mejores y peores activos por rentabilidad neta. (ranking ordenado por neto DESC; muestra mejores arriba y peores al final)
- [x] Cada monto permite abrir evidencia (drill-down 0074). (drillKey en Linea `ingreso_egreso:TIPO:APLICACION` y en ActivoRent `rentabilidad_activo:ID`; la pantalla de detalle es REQ-0074)
- [x] No se mezclan monedas. (ingreso_egreso es de la moneda base de la empresa -sin columna moneda-; base caja en una sola moneda; documentado)
- [x] Las reglas de clasificacion dependen de `articulo.aplicacion`, no de textos hardcodeados. (GROUP BY articulo.aplicacion; el mapeo a etiqueta es solo presentacion)
- [x] Los egresos asociados a una operacion/activo/persona conservan trazabilidad. (ingreso_egreso conserva operacion/activo/persona; el ranking usa ie.activo)

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
