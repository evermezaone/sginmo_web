# REQ-0074 - Drill-down de evidencia para indicadores del dashboard

**Numero:** REQ-0074
**Fecha de creacion:** 2026-07-12
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

Del usuario (2026-07-12): "todo resumen debe poder llevar a la evidencia en detalle de lo que se muestra... en un click se pueda ver esas propiedades."

## Objetivo Funcional

Implementar un mecanismo comun de evidencia para que cada KPI, grafico, subtotal u objetivo del dashboard
pueda abrir el detalle exacto que compone el numero mostrado.

## Criterios De Aceptacion

- [ ] Existe pantalla `dashboard-detalle.xhtml` o mecanismo equivalente para mostrar evidencia filtrada por indicador.
- [ ] Cada KPI del dashboard expone una clave de detalle y parametros firmes: periodo, moneda, sucursal, tipo, estado y otros filtros aplicados.
- [ ] El detalle de mora abre cuotas/clientes/documentos que componen el monto.
- [ ] El detalle de cobros abre cobros reales del periodo.
- [ ] El detalle de ingresos/egresos abre documentos o movimientos que componen cada subtotal.
- [ ] El detalle de ocupacion/vacancia abre propiedades que componen ocupado, vacante o brecha al objetivo.
- [ ] El detalle de rentabilidad abre ingresos y egresos vinculados al activo/operacion/persona correspondiente.
- [ ] La pantalla de detalle permite exportar CSV/PDF con filtros aplicados y sin mezclar monedas.
- [ ] Los enlaces de drill-down no aceptan rutas/query libres que permitan inyeccion JPQL/SQL; se usa whitelist de indicadores y filtros.
- [ ] El detalle respeta permisos: ver dashboard no necesariamente permite ver datos sensibles si el usuario no tiene permiso del modulo origen.

## Reglas De Negocio

- Si un resumen dice "10 propiedades faltantes", el detalle debe contener esas mismas 10 o explicar por que la lista cambio por datos actualizados.
- Cada detalle debe mostrar fecha/hora de generacion y filtros aplicados.
- El usuario debe poder volver al dashboard conservando filtros.

## Dependencias

- Depende de: REQ-0062, REQ-0069, REQ-0071, REQ-0072, REQ-0073.
- Requerido por: todos los dashboards gerenciales.

## Fuentes Y Trazabilidad

- Pedido directo del usuario sobre evidencia clicable de los resumenes.
- Estandar ABM: filtros/orden por whitelist.
