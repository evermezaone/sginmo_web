# REQ-0067 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-12
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos Bloqueantes

- El servicio y la tabla de auditoria funcional existen, pero la instrumentacion real de maestros sensibles no esta implementada salvo un ejemplo (`DESBLOQUEAR`). El REQ exige registrar altas, modificaciones, inactivaciones/reactivaciones y acciones criticas para registros sensibles; tener solo la API y una pantalla consultora no cumple el objetivo funcional.

## Solucion Esperada

- Cablear la auditoria en un primer conjunto minimo de maestros sensibles: `parametro_sistema`, `forma_pago`, `articulo`, `persona`, `activo`, `operacion`, `cobro`/anulacion y `plantilla_documento`, o redefinir formalmente el alcance del REQ.
- Para inactivaciones de maestros sensibles, exigir motivo desde la UI/Service y persistirlo en auditoria funcional.
- Agregar botones/pestanas de historial en ABM sensibles o, como minimo, links filtrados a la pantalla Auditoria por entidad/registro.

## Pruebas Revisadas

- Revision estatica de `AuditoriaFuncionalService`, `AuditoriaBean`, `auditoria.xhtml`, `SeguridadPoliticaService` y `V46__auditoria_funcional.sql`.
- Build Maven previo: `mvn -q clean package` EXIT 0.
