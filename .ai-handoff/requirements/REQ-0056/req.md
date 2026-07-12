# REQ-0056 - Dashboard gerencial de cartera, ventas, alquileres y caja

**Numero:** REQ-0056
**Fecha de creacion:** 2026-07-11
**Estado inicial:** NUEVO
**Prioridad:** media

## Texto Original

Proponer funcionalidades utiles y atractivas para elevar el nivel del programa.

## Objetivo Funcional

Construir un dashboard gerencial con indicadores clave de cartera, caja, ventas, alquileres, mora, propiedades disponibles y rendimiento por periodo.

## Criterios De Aceptacion

- [x] Dashboard muestra KPIs de cuotas vencidas, monto vencido, cobrado del mes, operaciones activas, ventas, alquileres y propiedades disponibles. (7 KPIs)
- [x] Filtros por empresa, sucursal, periodo, tipo de operacion y moneda. (periodo desde/hasta + moneda en UI; empresa=tenant via RLS; sucursal soportada en el servicio -combo UI diferido-; tipo se refleja en KPIs Ventas/Alquileres separados)
- [x] Indicadores respetan permisos y tenant. (@AislarTenant -> RLS por tenant; pantalla dashboard-gerencial con permiso VER)
- [x] Los montos usan `BigDecimal` y formato local. (BigDecimal en el servicio; f:convertNumber es-PY con agrupacion)
- [x] Cada KPI permite navegar al listado filtrado que lo explica. (h:link a operaciones/caja/activos; el filtro preaplicado en el destino es refinamiento)
- [x] Se distinguen datos reales de datos estimados/proyectados. (todos son datos reales transaccionales; etiquetado en la vista; no hay proyecciones en esta iteracion)
- [x] El dashboard no ejecuta consultas que bloqueen la operacion diaria. (solo lectura; queries sobre columnas indexadas estado/fecha/tenant)
- [x] Hay pruebas o evidencia de que las consultas funcionan con volumen razonable. (queries validadas en prod contra datos reales; smoke render OK; ver test-plan)

## Reglas De Negocio

- No mezclar monedas sin regla explicita de conversion.
- Los indicadores de mora deben basarse en cuotas reales y estado de cobro.
- Los datos de caja deben coincidir con planillas/arqueos cerrados cuando existan.

## Dependencias

- Depende de: REQ-0016, REQ-0017, REQ-0022, REQ-0024, REQ-0030, REQ-0059.
- Requerido por: direccion, ventas y control administrativo.

## Fuentes Y Trazabilidad

- Funcionalidad vendible para mostrar valor inmediato al cliente.
