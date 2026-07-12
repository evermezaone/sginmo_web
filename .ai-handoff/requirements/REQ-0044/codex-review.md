# REQ-0044 - Codex Review

## Decision

APROBADO_POR_CODEX.

## Revision

- Se confirmo que `personas.xhtml` ya no muestra el campo `Clasificacion fiscal`.
- La columna `persona_empresa.clasificacion_fiscal` permanece en BD/dominio, tal como fue decidido: no dropear en este REQ.
- El cambio no altera guardado ni validaciones del ABM Persona.

## Verificacion

- `mvn -q clean package`: OK.
