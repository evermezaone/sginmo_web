# REQ-0029 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-08
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Hallazgos

### Bloqueantes

- Ninguno pendiente.

### Observaciones cerradas

- Obs 237 cerrada en ronda 2: `caja.xhtml` ahora incluye el boton `Recaudacion (PDF)` con `ajax="false"`, visible solo con `sesionUsuario.puede('caja','EXPORTAR')`, invocando `descargaBean.recaudacion(cajaBean.planilla.id)`.

### No Bloqueantes

- `ReporteService.recaudacionPlanilla()` exige `autorizacion.exigir("caja", "EXPORTAR")`.
- `ReporteService.recaudacionPlanilla()` valida `planilla.empresa = empresaContexto` antes de armar el PDF.
- El PDF muestra cobros, apertura, cobrado y total en caja.
- La pantalla `ingresos-egresos.xhtml` tiene filtro por tipo (`Todos`, `Ingresos`, `Egresos`) y el servicio filtra por `empresa` y `tipo`.

## Riesgos

- Sin riesgos bloqueantes detectados para el alcance de REQ-0029.

## Pruebas Revisadas

- [x] Revision estatica de `ReporteService.recaudacionPlanilla`.
- [x] Revision estatica de `DescargaBean.recaudacion`.
- [x] Revision estatica del boton `Recaudacion (PDF)` en `caja.xhtml`.
- [x] Busqueda de invocaciones `recaudacion(` en Java/XHTML.
- [x] Revision estatica de `ingresos-egresos.xhtml`.
- [x] Revision estatica de `IngresoEgresoService.contar/listar`.
- [x] Build: `mvn -q clean package` en `Desarrollo` con EXIT 0.

## Pruebas Faltantes

- [ ] Prueba funcional manual: usuario con `caja/EXPORTAR` descarga PDF de recaudacion de su planilla; usuario sin `EXPORTAR` no ve/ejecuta el boton.
