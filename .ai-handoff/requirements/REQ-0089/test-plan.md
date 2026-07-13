# REQ-0089 - Plan De Pruebas

**Fecha:** 2026-07-13

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | BUILD OK | OK |
| T02 | Deploy VPS + smoke | personas 200; 36/36 | OK |

## Pruebas Manuales

| ID | Escenario | Pasos | Resultado esperado | Resultado real |
|---|---|---|---|---|
| M01 | Agregar rol al editar | Editar persona, agregar rol, Guardar, reabrir | El rol quedo asignado | pendiente |
| M02 | Quitar rol al editar | Editar persona, quitar rol, Guardar, reabrir | El rol ya no figura (baja logica) | pendiente |
| M03 | Alta+baja combinadas | Editar, agregar uno y quitar otro, Guardar | Ambos cambios persisten | pendiente |
| M04 | Persona nueva con roles | Crear persona, agregar roles, Guardar | Roles insertados | pendiente |

## Datos De Prueba

Una persona con al menos un rol asignado en la empresa activa.
