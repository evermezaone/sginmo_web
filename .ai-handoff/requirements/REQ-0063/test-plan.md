# REQ-0063 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | Build OK | OK |
| T02 | V44 en `BEGIN...ROLLBACK` | 7 plantillas + 29 permisos + pantalla | OK |
| T03 | Backup previo | dump OK | OK |
| T04 | Deploy + Flyway V44 | success=t | OK |
| T05 | `python tools/smoke-test-vps.py` | 29/29 render OK incl. roles-plantilla | OK (TODAS OK) |

## Pruebas Manuales

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | Elegir CAJA + grupo -> Ver diferencias | lista agregar/quitar | pendiente |
| M02 | Aplicar (add-only) | el grupo gana los permisos; no pierde los suyos | pendiente |
| M03 | Aplicar (reemplazar) | el grupo queda exactamente igual a la plantilla | pendiente |
| M04 | Verificar backend | un usuario del grupo ahora puede/no puede segun permiso_grupo | pendiente |

## Revision Transversal (SEGURIDAD)

- Autorizacion real: aplicar escribe permiso_grupo (lo que valida SesionUsuario.puede en el backend); no es solo UI.
- No superadmin: aplicar no toca usuario.perfil; solo (pantalla, accion).
- Aislamiento: aplicar/diff verifican grupo.tenant == tenant actual (grupo no tiene RLS).
- No borra sin confirmacion: add-only por defecto; reemplazar explicito con p:confirm.
- Auditoria: INSERT en permiso_grupo con usuario_creacion/fecha.

## Datos De Prueba

Un grupo del tenant. Las 7 plantillas base sembradas por V44.
