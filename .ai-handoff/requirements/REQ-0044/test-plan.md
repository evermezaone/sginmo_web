# REQ-0044 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package -DskipTests` | Build OK | OK |
| T02 | `python tools/smoke-test-vps.py` | 19/19 RENDER OK incl. `personas` | OK (RESULTADO: TODAS OK) |
| T03 | Render de `personas` sin el campo | la pagina responde 200 y ya no incluye el label "Clasificacion fiscal" | OK |

## Pruebas Manuales

| ID | Escenario | Pasos | Resultado esperado | Resultado real |
|---|---|---|---|---|
| M01 | Campo ausente | Personas -> nueva/editar -> pestana Datos | no aparece "Clasificacion fiscal" | pendiente (verificacion visual del usuario) |
| M02 | Alta/edicion sana | Crear y editar una persona y guardar | guarda sin errores | pendiente (verificacion del usuario) |

## Revision Transversal

- Cambio acotado a la vista `personas.xhtml`; no toca dominio, servicio, reglas compartidas
  (dinero/estados/cobros) ni BD. La columna `clasificacion_fiscal` sigue en `persona_empresa` y
  en la vista `v_persona`, por lo que ninguna lectura existente se rompe.
- No requiere migracion; ninguna otra pantalla referencia `clasificacionFiscal`
  (busqueda: `grep clasificacionFiscal webapp/` -> solo `personas.xhtml`, ya limpio).

## Datos De Prueba

Ninguno especial (cualquier persona existente sirve para la verificacion visual).
