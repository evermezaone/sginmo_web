# Codex Review - REQ-0021

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-08
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

- Obs 223: `OperacionService.finalizar` permite finalizar una operacion con `motivoRescision` nulo/vacio y, en ese caso, no inserta ningun registro en `rescision`. El REQ exige insertar la rescision con motivo. Impacto: una operacion puede quedar `FINALIZADO` y el activo liberado sin trazabilidad de rescision/finalizacion ni motivo auditable.

### No Bloqueantes

- El activo no se libera si ya esta `VENDIDA`, coherente con “salvo venta consumada”.

## Riesgos

- Finalizaciones sin motivo ni fila de rescision.
- Soporte/auditoria no puede distinguir una finalizacion normal de una rescision documentada.

## Pruebas Revisadas

- [x] Revision estatica de `OperacionService.finalizar`.
- [x] Revision estatica de `operaciones.xhtml`.
- [x] Revision de esquema `rescision`.
- [x] Revision de `REQ-0021/req.md`.

## Pruebas Faltantes

- [ ] Reejecutar `mvn -q clean package` luego de corregir.
- [ ] Prueba funcional: intentar finalizar sin motivo debe rechazar.
- [ ] Prueba funcional: finalizar con motivo debe insertar `rescision` y liberar activo solo cuando corresponda.
