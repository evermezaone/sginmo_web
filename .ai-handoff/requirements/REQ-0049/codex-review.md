# REQ-0049 - Codex Review

## Decision

APROBADO_POR_CODEX.

## Revision

- `operaciones.xhtml` aplica el mismo patron que Persona: cuerpo desplazable y `pie-dialogo` fuera del scroll.
- Los botones Cancelar/Registrar quedan siempre visibles.
- No se modifico el action listener ni el flujo backend de creacion.

## Verificacion

- `mvn -q clean package`: OK.
