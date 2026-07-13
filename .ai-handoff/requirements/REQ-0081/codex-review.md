# REQ-0081 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-13
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Hallazgos

### Bloqueantes

- Ninguno.

### No Bloqueantes

- El grid usa `minmax(360px, 1fr)`. En pantallas extremadamente angostas podria generar overflow horizontal del contenedor; no bloquea porque la tabla ya esta pensada con scroll horizontal y el objetivo principal era recuperar legibilidad.

## Validacion

- El layout de los paneles de caja paso a `repeat(auto-fit, minmax(360px, 1fr))`, evitando que la grilla quede fija a media pantalla en vistas con espacio disponible.
- La grilla de cobros queda dentro de un contenedor `overflow-x:auto`.
- La tabla define `min-width:34rem` y anchos/`white-space:nowrap` para columnas compactas, protegiendo encabezados, monto, estado y acciones.
- Se agrego columna `Fecha` usando `cb[1]` con `f:convertDateTime type="localDate" pattern="dd/MM/yyyy"`.
- `CajaService#cobrosDePlanilla()` ya devolvia `c.fecha` en `cb[1]`; no hubo cambio de backend ni datos.

## Riesgos

Riesgo bajo: cambio de presentacion localizado en `caja.xhtml`.

## Pruebas Revisadas

- [x] Revision estatica de `caja.xhtml`.
- [x] Revision de `CajaService#cobrosDePlanilla()` para confirmar disponibilidad de fecha.
- [x] `mvn -q -pl sginmo-web -am clean package` ejecutado desde `Desarrollo` con resultado EXIT 0.

## Pruebas Faltantes

- [ ] Prueba visual manual en escritorio con cobros reales.
- [ ] Prueba visual manual en ancho movil para confirmar scroll horizontal y paneles apilados.
