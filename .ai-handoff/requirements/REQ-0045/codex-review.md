# REQ-0045 - Codex Review

## Decision

APROBADO_POR_CODEX.

## Revision

- `personas.xhtml` usa cuerpo con scroll interno y pie de dialogo separado.
- Guardar/Cancelar quedan fuera del area desplazable, cumpliendo el criterio de botones siempre visibles.
- No se modifico backend ni contrato de guardado.

## Verificacion

- `mvn -q clean package`: OK.
