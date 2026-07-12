# REQ-0062 - Reportes exportables OpenPDF/CSV sin JasperReports

**Numero:** REQ-0062
**Fecha de creacion:** 2026-07-11
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

Proponer funcionalidades utiles y atractivas. No usamos ni usaremos Jasper.

## Objetivo Funcional

Reformular la capa de reportes para usar servicios propios, consultas controladas y exportacion OpenPDF/CSV/XML cuando aplique, sin JasperReports ni archivos `.jrxml`.

## Criterios De Aceptacion

- [ ] No se agrega dependencia JasperReports.
- [ ] No se agregan archivos `.jrxml`.
- [ ] Existe servicio comun de reportes con parametros tipados, permisos y salida PDF/CSV.
- [ ] Reportes iniciales: propiedades disponibles, cronograma de cuotas, cobros por periodo, mora, egresos/recaudacion y liquidaciones.
- [ ] Cada reporte tiene filtros visibles y los incluye en PDF.
- [ ] CSV usa encoding UTF-8 y separador documentado.
- [ ] Exportaciones grandes tienen limite o ejecucion controlada para no agotar memoria.
- [ ] Los reportes respetan tenant, permisos y filtros de sucursal.
- [ ] Cada reporte critico tiene prueba o evidencia de consulta con datos.

## Reglas De Negocio

- No mezclar monedas sin criterio explicito.
- Los reportes deben calcular montos con `BigDecimal`.
- Los reportes de cobros/anulaciones deben coincidir con la fuente transaccional, no con estimaciones de UI.

## Dependencias

- Depende de: REQ-0027, REQ-0028, REQ-0029, REQ-0030, REQ-0058.
- Requerido por: entrega comercial sin Jasper.

## Fuentes Y Trazabilidad

- Decision usuario: no usar ni usar JasperReports.
- Legacy: reportes existentes deben migrarse sin replicar bugs de montos.
