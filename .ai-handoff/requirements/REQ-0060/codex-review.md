# REQ-0060 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-12
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos Bloqueantes

- `ParametroService.guardar` crea parametros nuevos con `tenant=-1` cuando el valor viene nulo. La pantalla estandar no fuerza el tenant efectivo, por lo que el ABM tiende a crear/editar defaults globales en vez de parametros de empresa. Esto contradice la regla de prioridad tenant especifico sobre default global y el alcance de parametrizacion por empresa.
- No se observa un control diferenciado para que solo un perfil autorizado alto edite defaults globales (`tenant=-1`); el permiso general `parametros/EDITAR` alcanza.

## Solucion Esperada

- En alta normal, asignar el tenant efectivo para parametros de empresa.
- Permitir `tenant=-1` solo a SUPERADMIN o permiso global explicito.
- Mantener la resolucion efectiva `empresa -> global`, pero hacer que la UI/Service permita crear overrides de empresa reales.

## Pruebas Revisadas

- Revision estatica de `ParametroService`, `ParametroConfig`, `parametros.xhtml` y `V41__parametros_avanzados.sql`.
- Build Maven previo: `mvn -q clean package` EXIT 0.
