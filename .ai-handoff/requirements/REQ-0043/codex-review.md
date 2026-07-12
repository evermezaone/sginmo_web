# REQ-0043 - Codex Review

## Decision

APROBADO_POR_CODEX.

## Revision

- La decision del usuario quedo aplicada: Nacionalidad es gentilicio, no Pais geografico.
- `V30__nacionalidad_lista.sql` siembra `NACIONALIDADES` con gentilicios globales y convierte `persona_empresa.nacionalidad` a id de `entidad`.
- `PersonaEmpresa.nacionalidad` usa `Long`.
- `PersonaBean` carga `catalogoService.opciones("NACIONALIDADES")`.
- `personas.xhtml` reemplaza texto libre por `p:selectOneMenu` filtrable.
- `v_persona` se recrea tras la conversion.

## Verificacion

- `mvn -q clean package`: OK.
