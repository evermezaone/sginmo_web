# REQ-0028 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-08
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Hallazgos

### Bloqueantes

- Ninguno pendiente.

### Observaciones cerradas

- Obs 236 cerrada en ronda 2: `DescargaBean` ahora toma la empresa desde `ContextoEmpresa` y la pasa a `ReporteService`. `reciboCobro()` valida `cobro.empresa = empresaContexto`; `estadoCuenta()` valida `operacion.empresa = empresaContexto` antes de emitir el PDF. Si el ID pertenece a otra empresa, la consulta no devuelve cabecera y se informa como inexistente.

### No Bloqueantes

- `ReporteService.reciboCobro()` exige `autorizacion.exigir("caja", "EXPORTAR")`.
- `ReporteService.estadoCuenta()` exige `autorizacion.exigir("operaciones", "EXPORTAR")`.
- Los botones PDF en `caja.xhtml` y `operaciones.xhtml` estan protegidos con `sesionUsuario.puede(..., 'EXPORTAR')`.
- El estado de cuenta usa `v_operacion_saldo` y `f_mora_cuota(..., current_date)`, consistente con el objetivo funcional.

## Riesgos

- Sin riesgos bloqueantes detectados para el alcance de REQ-0028.

## Pruebas Revisadas

- [x] Revision estatica de `ReporteService.reciboCobro`.
- [x] Revision estatica de `ReporteService.estadoCuenta`.
- [x] Revision estatica de `DescargaBean.recibo` y `DescargaBean.estadoCuenta`.
- [x] Revision estatica de botones PDF en `caja.xhtml` y `operaciones.xhtml`.
- [x] Comparacion con `ContextoEmpresa`, `CajaService`, `OperacionService` y docs de multiempresa.
- [x] Busqueda de llamadas antiguas a firmas de reportes.
- [x] Build: `mvn -q clean package` en `Desarrollo` con EXIT 0.

## Pruebas Faltantes

- [ ] Prueba funcional manual con usuario de empresa A intentando descargar recibo/cuenta de empresa B por ID directo.
