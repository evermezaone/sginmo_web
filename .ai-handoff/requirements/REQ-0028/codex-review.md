# REQ-0028 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-08
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

- Obs 236: los reportes PDF de recibo de cobro y estado de cuenta se generan por ID sin validar que el cobro/operacion pertenezca a la empresa del contexto del usuario. `DescargaBean.recibo()` y `DescargaBean.estadoCuenta()` solo pasan `cobroId`/`operacionId` y usuario; `ReporteService.reciboCobro()` consulta `cobro WHERE cobro = :c`, y `ReporteService.estadoCuenta()` consulta `operacion WHERE operacion = :o`, sin filtro por empresa. Aunque ahora se exige `EXPORTAR`, un usuario con permiso de caja/operaciones puede intentar descargar PDFs de otra empresa si conoce o adivina el ID. El estandar del proyecto mapea `VARIABLES_ENTORNOS` a sesion y filtro por `empresa_id` en consultas (`docs-migracion/06-propuesta-stack-web.md`), y el legado declara multi-tenancy por empresa/sucursal activa (`docs-migracion/01-stack-actual.md`).

### No Bloqueantes

- `ReporteService.reciboCobro()` exige `autorizacion.exigir("caja", "EXPORTAR")`.
- `ReporteService.estadoCuenta()` exige `autorizacion.exigir("operaciones", "EXPORTAR")`.
- Los botones PDF en `caja.xhtml` y `operaciones.xhtml` estan protegidos con `sesionUsuario.puede(..., 'EXPORTAR')`.
- El estado de cuenta usa `v_operacion_saldo` y `f_mora_cuota(..., current_date)`, consistente con el objetivo funcional.

## Riesgos

- Exposicion cruzada de recibos, cronogramas, saldos, mora y datos de clientes entre empresas por descarga directa con IDs.

## Pruebas Revisadas

- [x] Revision estatica de `ReporteService.reciboCobro`.
- [x] Revision estatica de `ReporteService.estadoCuenta`.
- [x] Revision estatica de `DescargaBean.recibo` y `DescargaBean.estadoCuenta`.
- [x] Revision estatica de botones PDF en `caja.xhtml` y `operaciones.xhtml`.
- [x] Comparacion con `ContextoEmpresa`, `CajaService`, `OperacionService` y docs de multiempresa.
- [x] Build ejecutado durante la ronda: `mvn -q clean package` en `Desarrollo` con EXIT 0.

## Pruebas Faltantes

- [ ] Prueba funcional con usuario de empresa A intentando descargar recibo/cuenta de empresa B por ID directo; debe fallar con mensaje de negocio o 403.

## Solucion Esperada

- Pasar la empresa del `ContextoEmpresa` desde `DescargaBean` a `ReporteService` o resolverla en backend desde el usuario autenticado.
- En `reciboCobro`, validar que `cobro.empresa = empresaContexto` antes de armar cabecera/detalle.
- En `estadoCuenta`, validar que `operacion.empresa = empresaContexto` antes de armar cabecera/cronograma.
- Mantener el enforcement `EXPORTAR` actual; el control de permiso no reemplaza el aislamiento por empresa.
