# Codex Review - REQ-0018

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-07
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

- Obs 218: la comision de alquiler se calcula sobre `op.getPrecio()`, pero RN-OPE-002 exige `garantia * %comision / 100` para ALQUILER y `precio * %comision / 100` para VENTA. Si garantia difiere del precio, el egreso de comision queda mal calculado.
- Obs 219: los movimientos automaticos de comision y garantia se registran como documentos internos `DINT` con `direccion_dinero = 'ENTRADA'`. RN-OPE-012/013 y el backlog de REQ-0018 exigen `ingreso_egreso`: comision como `EGRESO` con item `COMISION_ALQUILER`/`COMISION_VENTA`, y deposito de garantia como `INGRESO` con item `DEPOSITO_GARANTIA`, estado `CANCELADO`. El esquema y los articulos seed ya existen para eso.

### No Bloqueantes

- La validacion visual/manual sigue pendiente en el handoff, pero queda eclipsada por los bloqueos de negocio.

## Riesgos

- Importes de comision incorrectos en alquiler.
- Reportes/liquidaciones/ingresos-egresos no veran los movimientos automaticos porque se crean como documentos genericos, no como movimientos clasificados.
- La comision queda tratada como entrada, aunque la regla documentada la define como egreso.

## Pruebas Revisadas

- [x] Revision estatica de `OperacionService.crear`.
- [x] Revision de `docs-migracion/03-reglas-negocio-nucleo.md` RN-OPE-002, RN-OPE-012 y RN-OPE-003/013.
- [x] Revision de `docs-migracion/08-backlog-reqs.md` para alcance de REQ-0018.
- [x] Revision de seed de articulos `COMISION_VENTA`, `COMISION_ALQUILER`, `DEPOSITO_GARANTIA`.

## Pruebas Faltantes

- [ ] Reejecutar `mvn -q clean package` luego de corregir.
- [ ] Prueba funcional que verifique registros en `ingreso_egreso` con tipo/item/estado correctos y monto correcto para alquiler y venta.
