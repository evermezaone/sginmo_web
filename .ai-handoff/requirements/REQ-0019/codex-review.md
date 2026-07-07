# Codex Review - REQ-0019

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-07
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

- Obs 220: la regeneracion de cuotas se autoriza con `operaciones/EDITAR` en backend y UI, pero la fuente del REQ indica `solo ADMINISTRADOR` (`docs-migracion/08-backlog-reqs.md` y `docs-migracion/03-reglas-negocio-nucleo.md` para `FrmRegenerarCuotas`). Es una accion correctiva sensible y no debe quedar habilitada para cualquier editor de operaciones.
- Obs 221: `OperacionService.regenerarCuotas` actualiza `plazo`, pero no recalcula `fechaFinContrato`. La regla documentada para `FrmRegenerarCuotas` indica que si cambia el plazo de una operacion se recalcula `FECHA_FIN_CONTRATO` y se regenera el cronograma completo.

### No Bloqueantes

- La guarda de BD en `f_generar_cronograma` evita borrar cuotas con saldo distinto del monto, cubriendo el caso principal de cuotas con cobros activos/parciales.

## Riesgos

- Usuarios con permiso general de edicion podrian regenerar cuotas sin perfil administrador.
- Contratos quedan con cronograma regenerado pero fecha fin contractual desfasada.

## Pruebas Revisadas

- [x] Revision estatica de `OperacionService.regenerarCuotas`.
- [x] Revision estatica de `operaciones.xhtml`.
- [x] Revision estatica de `V16__motor_documento.sql`.
- [x] Revision de `docs-migracion/08-backlog-reqs.md`.
- [x] Revision de `docs-migracion/03-reglas-negocio-nucleo.md`.

## Pruebas Faltantes

- [ ] Reejecutar `mvn -q clean package` luego de corregir.
- [ ] Prueba funcional con usuario no administrador.
- [ ] Prueba funcional que cambie cantidad/primera fecha y verifique `fecha_fin_contrato`.
