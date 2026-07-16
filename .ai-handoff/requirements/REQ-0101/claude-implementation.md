# REQ-0101 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-16
**Rama:** multiempresa

## Manifiesto Minimo Para Codex
- REQ: REQ-0101
- Tipo de cambio: UI (CSS)
- Riesgo: minimo
- Archivos clave:
  - `webapp/WEB-INF/portal-acceso.xhtml`: .campo-a .ui-password display:block width:100%.
- Comandos probados:
  - `python xml.dom.minidom.parse` portal-acceso.xhtml: OK.
  - `powershell -File tools/deploy-vps.ps1`: Build OK / Redeploy OK.
  - `python tools/smoke-test-vps.py`: TODAS OK.
- Cambios de datos: no | entorno: no
- Decision esperada: aprobar

## Resumen Tecnico
El p:password (toggleMask) se renderiza como span.ui-password inline; se le da display:block width:100% para igualar al resto.

## Archivos Modificados
| Archivo | Cambio |
|---|---|
| `webapp/WEB-INF/portal-acceso.xhtml` | CSS ancho del p:password |

## Cambios De Datos
Sin cambios.
## Variables De Entorno
Sin cambios.
## Pruebas Ejecutadas
- portal-acceso.xhtml XML OK; Build OK; smoke 37/37.
## Pruebas Manuales Sugeridas
1. Abrir portal/login -> el campo Contrasena tiene el mismo ancho que Documento.
## Riesgos Conocidos
Ninguno.
