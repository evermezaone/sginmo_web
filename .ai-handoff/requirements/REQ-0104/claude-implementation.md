# REQ-0104 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-16
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0104
- Tipo de cambio: UI (CSS de una pantalla)
- Riesgo: bajo (solo presentacion)
- Archivos clave:
  - `Desarrollo/sginmo-web/src/main/webapp/index.xhtml`: CSS de `.kpi .n` en el bloque `<style>` del Inicio.
- Comandos probados:
  - `powershell -File tools/deploy-vps.ps1`: Build OK + Redeploy OK + HTTP 200 login (build+deploy).
- Cambios de datos: no.
- Cambios de entorno: no.
- Decision esperada: aprobar.
- Notas para auditor: se usa `container-type: inline-size` en `.kpi` y `font-size: clamp(0.95rem, 11cqi, 1.6rem)` en `.kpi .n`. Container queries: soportado por navegadores modernos (el sistema se usa en Chrome). Fallback si un navegador viejo no soporta `cqi`: el `clamp` cae al valor preferido invalido y usa 1.6rem (comportamiento previo), sin romper nada.

## Resumen Funcional

En el Inicio, los montos grandes (ej. "1.225.995.000" en "Saldo por cobrar") ya no desbordan la tarjeta:
el numero se escala al ancho del recuadro. Los KPI chicos (0, 40, 45) mantienen su tamano (tope 1.6rem).

## Resumen Tecnico

`.kpi` pasa a ser contenedor de consulta (`container-type: inline-size`) y `.kpi .n` usa
`font-size: clamp(0.95rem, 11cqi, 1.6rem)` + `white-space: nowrap`. `11cqi` = 11% del ancho de la
tarjeta, asi el tipo se adapta al espacio real disponible (no al viewport), con piso 0.95rem y techo 1.6rem.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| `Desarrollo/sginmo-web/src/main/webapp/index.xhtml` | CSS `.kpi` (container-type) y `.kpi .n` (font-size clamp con cqi + nowrap). |

## Cambios De Datos

Sin cambios.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

- Build + deploy a la VPS con `tools/deploy-vps.ps1` (Build OK / Redeploy OK / HTTP 200).

## Pruebas Manuales Sugeridas

1. Entrar al Inicio con datos reales de Pysistemas: la tarjeta "Saldo por cobrar" muestra el monto completo dentro del recuadro.
2. Reducir el ancho de la ventana: el numero se sigue escalando sin desbordar.

## Riesgos Conocidos

Ninguno funcional. En navegadores muy viejos sin soporte de container queries, el KPI vuelve al tamano fijo previo (1.6rem), que es el comportamiento anterior.
