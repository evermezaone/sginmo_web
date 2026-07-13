# REQ-0079 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-13
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Hallazgos

### Bloqueantes

- Ninguno.

### No Bloqueantes

- Ninguno.

## Validacion

- La accion destructiva de anulacion queda oculta por defecto; el boton normal de la grilla solo permite imprimir.
- El modo de anulacion se habilita con el control separado "Anular ultimo cobro..." y muestra advertencia, motivo obligatorio y confirmacion.
- La X roja se renderiza solo si el usuario tiene `caja/INACTIVAR`, el modo esta activo y el cobro coincide con `cobroAnulableId`.
- `CajaBean#getCobroAnulableId()` limita el candidato al ultimo cobro `ACTIVO` de la planilla cargada y de la fecha actual.
- `CajaService#anularCobro()` vuelve a validar en backend permiso, motivo, existencia, estado `ACTIVO`, fecha igual a hoy y que no exista un cobro activo posterior en la misma planilla.
- La anulacion delega en `f_anular_cobro` y registra auditoria funcional con accion `ANULAR` y motivo.

## Riesgos

Ninguno identificado.

## Pruebas Revisadas

- [x] Revision estatica de `CajaService`, `CajaBean`, `caja.xhtml`, `AuditoriaFuncionalService` y migraciones relacionadas.
- [x] `mvn -q -pl sginmo-web -am clean package` ejecutado desde `Desarrollo` con resultado EXIT 0.

## Pruebas Faltantes

- [ ] Prueba manual en UI con caja abierta y cobros reales.
