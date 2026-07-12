# REQ-0070 - Dashboard gerencial visual con graficos y evoluciones

**Numero:** REQ-0070
**Fecha de creacion:** 2026-07-12
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

Del usuario (2026-07-12): "graficos, evoluciones, por tipos de ingresos."

## Objetivo Funcional

Rediseñar el dashboard gerencial para que sea una pantalla ejecutiva rica: KPIs comparativos,
graficos de evolucion, composicion por tipos y lectura rapida de tendencia, sin perder el caracter
operativo del sistema.

## Criterios De Aceptacion

- [ ] La pantalla mantiene un resumen ejecutivo superior con KPIs clave y variacion contra comparativos.
- [ ] Incluye grafico de linea de evolucion mensual de los ultimos 12 meses para cobros, mora, ingresos, egresos y rentabilidad.
- [ ] Incluye grafico de barras de ingresos vs egresos por mes.
- [ ] Incluye grafico de distribucion por tipo de ingreso: alquiler, venta, comision, mora/interes, otros.
- [ ] Incluye grafico o tabla de ocupacion/vacancia por tipo de activo, zona/sucursal y estado.
- [ ] Todos los graficos respetan los filtros de periodo, moneda, sucursal, tipo de activo y tipo de operacion cuando apliquen.
- [ ] Cada punto/barra/segmento debe tener una accion de detalle o enlace a evidencia filtrada (REQ-0074).
- [ ] La UI usa componentes PrimeFaces/Chart.js disponibles en el stack; no agrega JasperReports ni dependencia pesada innecesaria.
- [ ] La pantalla es responsive y legible en escritorio y movil; no debe usar textos que se solapen ni tarjetas anidadas.
- [ ] No se muestran graficos vacios sin explicacion: debe diferenciar "sin datos" de "sin resultados por filtros".

## Reglas De Negocio

- Los graficos monetarios no mezclan monedas.
- Las evoluciones deben calcularse desde datos transaccionales, no desde snapshots manuales, salvo que se defina una tabla resumen auditada.
- Los colores de tendencia deben tener semantica consistente: verde mejora, rojo empeora, gris sin base comparable.

## Dependencias

- Depende de: REQ-0069.
- Requerido por: direccion, gerencia y demostracion comercial.

## Fuentes Y Trazabilidad

- Pedido directo del usuario sobre graficos/evoluciones.
- Estandar frontend JSF/PrimeFaces.
