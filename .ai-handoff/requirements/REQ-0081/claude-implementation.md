# REQ-0081 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-13
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0081
- Tipo de cambio: UI (caja.xhtml). Sin backend, sin BD.
- Riesgo: bajo (presentacion).
- Archivos clave:
  - `webapp/caja.xhtml`:
    - El grid de los dos paneles pasa de `grid-template-columns: 1fr 1fr` a `repeat(auto-fit, minmax(360px, 1fr))` -> responsive; en pantallas angostas los paneles se apilan y la grilla usa el ancho completo.
    - La `p:dataTable` de cobros se envuelve en `div overflow-x:auto` con `min-width:34rem`; columnas con anchos y `white-space:nowrap` (# / Fecha / Monto / Estado / Acciones) para no truncar; Cliente/Forma con `min-width`.
    - Se agrega la columna **Fecha** (`cb[1]`, dd/MM/yyyy) — dato ya disponible; util para ver el cobro del dia (REQ-0079).
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - Deploy VPS; `python tools/smoke-test-vps.py`: 36/36 (caja 200).
- Cambios de datos: no. Cambios de entorno: no.
- Decision esperada: aprobar; es solo presentacion.
- Notas para auditor:
  - El backend `cobrosDePlanilla` ya devolvia la fecha en `cb[1]`; no cambia el servicio.
  - `f:convertDateTime type="localDate"` (igual que la grilla de cuotas) porque el driver devuelve LocalDate.

## Resumen Funcional

La grilla "Cobros de la planilla" ahora se lee bien: encabezados completos, monto sin cortarse, y columna Fecha; en pantallas chicas los paneles se apilan y/o la grilla scrollea.

## Resumen Tecnico

Layout responsive de los paneles + contenedor con scroll horizontal y anchos/nowrap en las columnas de la grilla de cobros.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| webapp/caja.xhtml | grid responsive + tabla de cobros con scroll, anchos/nowrap y columna Fecha |

## Cambios De Datos

Sin cambios.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; deploy VPS; smoke 36/36 (caja 200).

## Pruebas Manuales Sugeridas

1. Abrir caja con cobros en pantalla ancha: la grilla muestra #, Fecha, Cliente, Monto (completo), Forma, Estado, Acciones sin truncar.
2. Reducir el ancho de la ventana: los paneles se apilan; si aun es angosto, la grilla scrollea horizontalmente sin romper la pagina.

## Limitaciones Conocidas

- Ninguna.

## Riesgos Conocidos

- Ninguno (cambio de presentacion).
