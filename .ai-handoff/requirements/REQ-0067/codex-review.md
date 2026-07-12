# REQ-0067 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-12
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos Bloqueantes

- Hay avance respecto de la ronda anterior: `forma_pago`, `articulo` y `parametro_sistema` ya registran altas/modificaciones/inactivaciones o reactivaciones, y `DESBLOQUEAR` sigue cableado como accion critica de usuario.
- Sigue incompleto contra el alcance del propio REQ: los maestros sensibles iniciales incluyen `persona`, `moneda`, `usuario`, `activo`, `operacion`, `cuota`, `cobro` y `plantilla_documento`, y las acciones fuertes `cobro`, `anulacion`, `descuento`, `liquidacion` y `regeneracion` aun no aparecen instrumentadas.
- Las inactivaciones de `forma_pago` y `articulo` registran motivo automatico tipo `estado ACTIVO -> INACTIVO`, pero no exigen motivo de negocio desde UI/Service para maestros sensibles, a pesar de que el REQ pide motivo obligatorio cuando aplique.

## Solucion Esperada

- Completar la instrumentacion minima en los registros sensibles enumerados por el REQ, al menos `persona`, `moneda`, `usuario`, `activo`, `operacion`, `cuota`, `cobro`/anulacion y `plantilla_documento`, o ajustar formalmente el alcance del REQ.
- Instrumentar acciones criticas: cobrar, anular, descuento, liquidar y regenerar.
- Para inactivaciones de maestros sensibles, exigir motivo de negocio desde UI/Service y persistirlo en auditoria funcional.
- Agregar botones/pestanas de historial en ABM sensibles o, como minimo, links filtrados a la pantalla Auditoria por entidad/registro.

## Pruebas Revisadas

- Revision estatica de `AuditoriaFuncionalService`, `AuditoriaBean`, `auditoria.xhtml`, `SeguridadPoliticaService` y `V46__auditoria_funcional.sql`.
- Build Maven previo: `mvn -q clean package` EXIT 0.
