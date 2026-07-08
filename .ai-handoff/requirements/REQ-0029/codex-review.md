# REQ-0029 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-08
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

- Obs 237: la recaudacion de planilla existe en backend (`DescargaBean.recaudacion()` y `ReporteService.recaudacionPlanilla()`), pero no hay boton ni accion XHTML que la invoque. La busqueda de `recaudacion(` solo encuentra Java, no `caja.xhtml` u otra vista. Impacto: el usuario no puede generar desde la pantalla el PDF de recaudacion requerido por el REQ, aunque el servicio exista. Solucion esperada: agregar en la pantalla de caja/planilla un boton PDF de recaudacion visible solo con `sesionUsuario.puede('caja','EXPORTAR')`, `ajax="false"`, invocando `descargaBean.recaudacion(cajaBean.planilla.id)` o equivalente.

### No Bloqueantes

- `ReporteService.recaudacionPlanilla()` exige `autorizacion.exigir("caja", "EXPORTAR")`.
- `ReporteService.recaudacionPlanilla()` valida `planilla.empresa = empresaContexto` antes de armar el PDF.
- El PDF muestra cobros, apertura, cobrado y total en caja.
- La pantalla `ingresos-egresos.xhtml` tiene filtro por tipo (`Todos`, `Ingresos`, `Egresos`) y el servicio filtra por `empresa` y `tipo`.

## Riesgos

- Funcionalidad backend no accesible desde la UI, por lo que el criterio funcional de reporte PDF queda incompleto.

## Pruebas Revisadas

- [x] Revision estatica de `ReporteService.recaudacionPlanilla`.
- [x] Revision estatica de `DescargaBean.recaudacion`.
- [x] Busqueda de invocaciones `recaudacion(` en Java/XHTML.
- [x] Revision estatica de `ingresos-egresos.xhtml`.
- [x] Revision estatica de `IngresoEgresoService.contar/listar`.
- [x] Build ejecutado durante la ronda: `mvn -q clean package` en `Desarrollo` con EXIT 0.

## Pruebas Faltantes

- [ ] Reejecutar `mvn -q clean package` luego de agregar el boton.
- [ ] Prueba funcional: usuario con `caja/EXPORTAR` debe poder descargar PDF de recaudacion de su planilla; usuario sin `EXPORTAR` no debe ver/ejecutar el boton.
