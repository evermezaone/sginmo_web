# REQ-0098 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-14
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0098
- Tipo de cambio: UI
- Riesgo: minimo (se elimina un h2)
- Archivos clave:
  - `webapp/portal/inicio.xhtml`: se quito el h2 "Como cliente" (rendered esPropietario). Se conserva "Como propietario".
- Comandos probados:
  - `python xml.dom.minidom.parse inicio.xhtml`: OK.
  - `powershell -File tools/deploy-vps.ps1`: Build OK / Redeploy OK, EXIT: 0.
  - `python tools/smoke-test-vps.py`: === RESULTADO: TODAS OK ===.
- Cambios de datos: no | Cambios de entorno: no
- Decision esperada: aprobar
- Notas para auditor: cambio puramente visual; no toca autorizacion ni secciones.

## Resumen Tecnico
Se elimino la linea del h2 "Como cliente" del bloque esCliente de inicio.xhtml. El saludo "Bienvenido, {nombre}"
(REQ-0095) queda como unico encabezado; "Como propietario" se conserva como divisor para dual-rol.

## Archivos Modificados
| Archivo | Cambio |
|---|---|
| `webapp/portal/inicio.xhtml` | eliminado el h2 "Como cliente" |

## Cambios De Datos
Sin cambios.
## Variables De Entorno
Sin cambios.
## Pruebas Ejecutadas
- inicio.xhtml XML OK; Build OK; deploy Redeploy OK; smoke 37/37.
## Pruebas Manuales Sugeridas
1. Entrar al portal como cliente y como cliente+propietario -> no aparece "Como cliente"; el saludo se mantiene.
## Riesgos Conocidos
Ninguno.
