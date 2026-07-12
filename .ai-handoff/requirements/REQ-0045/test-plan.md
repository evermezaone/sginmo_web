# REQ-0045 - Plan De Pruebas

**Fecha:** 2026-07-11

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package -DskipTests` | Build OK | OK |
| T02 | `python tools/smoke-test-vps.py` | 19/19 RENDER OK incl. `personas` | OK (RESULTADO: TODAS OK) |
| T03 | Render de `personas` con el dialogo | la pagina responde 200 sin error de render tras el cambio de layout | OK |

## Pruebas Manuales

| ID | Escenario | Pasos | Resultado esperado | Resultado real |
|---|---|---|---|---|
| M01 | Botones al pie | Login → Personas → Nuevo | Guardar/Cancelar visibles al pie; cuerpo con scroll interno | pendiente (verificacion visual del usuario) |
| M02 | Guardado intacto | Editar persona con roles y Guardar | Se guarda sin errores; grilla se refresca | pendiente (verificacion visual del usuario) |

## Revision Transversal

- Solo se toco `personas.xhtml` (presentacion). No hay logica de negocio compartida afectada.
- El patron de pie fijo con clase `pie-dialogo` es reutilizable en otros dialogos largos
  (ver REQ-0048/0049 sobre scroll de dialogo de operacion).

## Datos De Prueba

Ninguno especial. Se usan personas existentes del entorno.
