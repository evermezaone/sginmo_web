# REQ-0042 - Implementacion Codex

**Fecha:** 2026-07-10T16:22:56-03:00

## Cambios

- `GeografiaService` incorpora consultas de combos por nivel y por padre.
- `ActivoBean` maneja cascada Pais -> Departamento -> Ciudad -> Barrio y guarda en `activo.ubicacion` el nivel mas especifico seleccionado.
- Al editar un activo, el bean reconstruye la cascada desde la ubicacion guardada.
- Propietarios puede usarse en alta con filas temporales, y se persisten al guardar el activo.
- `ActivoService.loteamientos()` lista un combo cerrado de activos tipo `LOTEAMIENTO`.
- `activos.xhtml` reemplaza autocomplete de loteamiento por `p:selectOneMenu`.

## Verificacion

- `mvn -q clean package` ejecutado desde `migracion\Desarrollo`: EXIT 0.

