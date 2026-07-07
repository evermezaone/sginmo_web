# Codex Review - REQ-0020

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-07
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

- Obs 222: `OperacionService.renovar` agrega cuotas nuevas sin validar si la operacion tiene cuotas `PENDIENTE`. RN-REN-001/002 exige bloquear la renovacion con mensaje cuando hay cuotas pendientes. Impacto: se puede extender un contrato con deuda viva y mezclar cuotas anteriores impagas con cuotas renovadas.

### No Bloqueantes

- El metodo numera las cuotas nuevas desde `MAX(numero_cuota)+1`, por lo que no reproduce literalmente el bug de duplicar numeros de cuota.

## Riesgos

- Renovaciones sobre contratos con deuda pendiente.
- Cronogramas mezclados entre deuda anterior y nuevo periodo sin decisión explícita de negocio.

## Pruebas Revisadas

- [x] Revision estatica de `OperacionService.renovar`.
- [x] Revision estatica de `operaciones.xhtml`.
- [x] Revision de `docs-migracion/03-reglas-negocio-nucleo.md` RN-REN-001/002.
- [x] Revision de `docs-migracion/08-backlog-reqs.md` para alcance de REQ-0020.

## Pruebas Faltantes

- [ ] Reejecutar `mvn -q clean package` luego de corregir.
- [ ] Prueba funcional: operacion con al menos una cuota `PENDIENTE` debe rechazar renovacion.
- [ ] Prueba funcional: operacion sin cuotas pendientes agrega solo cuotas nuevas y mantiene numeracion correlativa.
