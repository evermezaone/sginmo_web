# REQ-0096 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-14
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0096
- Tipo de cambio: UI (CSS)
- Riesgo: bajo (solo estilos del template del portal; sin backend)
- Archivos clave:
  - `webapp/WEB-INF/portal.xhtml`: media queries + overflow-x por seccion + header flex-wrap + grid responsive.
- Comandos probados:
  - `powershell -File tools/deploy-vps.ps1` (mvn clean package + deploy): Build OK / Redeploy OK, EXIT: 0.
  - `python tools/smoke-test-vps.py`: === RESULTADO: TODAS OK === (37 pantallas 200).
- Cambios de datos: no
- Cambios de entorno: no
- Impacto LLM/tokens: no
- Decision esperada: aprobar
- Notas para auditor: el template cubre inicio.xhtml y transferencia.xhtml; el scroll horizontal se aisla
  por .seccion-p (celdas nowrap) para no romper la pagina en mobile.

## Resumen Funcional

El portal del socio se ve bien en celular: sin desbordes, header ordenado, tarjetas 2x2 y tablas que
scrollean dentro de su tarjeta.

## Resumen Tecnico

Se ampliaron los estilos del template WEB-INF/portal.xhtml: box-sizing global, header con flex-wrap y
acciones que envuelven, grid de tarjetas a 2/1 columnas segun ancho, .seccion-p con overflow-x:auto y
celdas nowrap, y media queries en 640px/380px. h1 de bienvenida escalado en mobile.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| `webapp/WEB-INF/portal.xhtml` | CSS responsive (header, tarjetas, tablas, media queries) |

## Cambios De Datos

Sin cambios.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

- Build OK (mvn clean package, EXIT 0).
- Deploy a la VPS: Redeploy OK; login.xhtml HTTP 200.
- smoke-test-vps.py: 37 pantallas HTTP 200 (TODAS OK).

## Pruebas Manuales Sugeridas

1. Abrir el portal en un celular (o DevTools responsive ~360px): sin scroll horizontal de pagina;
   tarjetas 2x2; tablas de cuotas/pagos scrollean dentro de su tarjeta; header ordenado.

## Riesgos Conocidos

Ninguno. En escritorio el layout se mantiene (max 900px).
