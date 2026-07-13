# REQ-0082 - Plan De Pruebas

**Fecha:** 2026-07-13

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `curl -i /sginmo-web/portal/` | 302 Location portal/login.xhtml | OK |
| T02 | `curl -i /sginmo-web/portal/index.xhtml` | 302 Location portal/login.xhtml | OK |
| T03 | smoke render | 36/36 | OK |

## Pruebas Manuales

| ID | Escenario | Pasos | Resultado esperado | Resultado real |
|---|---|---|---|---|
| M01 | Sesion de portal activa | Loguear en portal y abrir /portal/ | Redirige a portal/inicio | pendiente |

## Datos De Prueba

Ninguno especial.
