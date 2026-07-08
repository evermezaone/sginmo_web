# REQ-0027 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-08
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Hallazgos

### Bloqueantes

- Ninguno pendiente.

### Observaciones cerradas

- Obs 235 cerrada en ronda 2: el PDF de activos/propiedades ahora exige permiso `EXPORTAR` en UI y backend. `activos.xhtml` renderiza el boton PDF solo con `sesionUsuario.puede('activos','EXPORTAR')`; `ReporteService.listadoActivos()` ejecuta `autorizacion.exigir("activos", "EXPORTAR")` antes de generar el PDF.

### No Bloqueantes

- El reporte usa OpenPDF via `PdfService`, consistente con el paquete de reportes estandar vigente.
- El boton usa `ajax=false`, adecuado para descarga.
- La consulta incluye nombre, tipo, precios y situacion como pide el REQ.
- Claude aplico el mismo patron de permiso `EXPORTAR` en los otros reportes PDF visibles (`caja`, `operaciones`, `recaudacion`), lo que reduce riesgo de divergencia para los REQ siguientes.

## Riesgos

- Sin riesgos bloqueantes detectados para el alcance de REQ-0027.

## Pruebas Revisadas

- [x] Revision estatica de `ReporteService.listadoActivos`.
- [x] Revision estatica de `DescargaBean.listadoActivos`.
- [x] Revision estatica de `activos.xhtml`.
- [x] Comparacion con estandar de permisos de exportacion en ABM.
- [x] Verificacion de enforcement uniforme en `ReporteService.reciboCobro`, `estadoCuenta`, `recaudacionPlanilla` y `listadoActivos`.
- [x] Build: `mvn -q clean package` en `Desarrollo` con EXIT 0.

## Pruebas Faltantes

- [ ] Prueba funcional manual con usuario que tenga `VER` sin `EXPORTAR` y usuario con `EXPORTAR`.
