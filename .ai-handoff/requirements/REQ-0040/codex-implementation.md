# REQ-0040 - Implementacion Codex

**Fecha:** 2026-07-10T16:22:56-03:00

## Cambios

- `PersonaBean` ahora expone `tituloDialogo`, calculado por `tipoPersoneria`.
- La pestana Roles ya no queda bloqueada para registros nuevos.
- `PersonaBean.agregarRol()` permite roles temporales antes de que exista `persona.id`.
- Al guardar una persona nueva, los roles temporales se persisten con `PersonaService.agregarRol`.
- `quitarRol` soporta tanto roles persistidos como roles temporales.

## Verificacion

- `mvn -q clean package` ejecutado desde `migracion\Desarrollo`: EXIT 0.

