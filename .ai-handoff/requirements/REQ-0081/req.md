# REQ-0081 - Legibilidad de la grilla de cobros en caja: columnas apretadas, monto y encabezados truncados

**Numero:** REQ-0081
**Fecha de creacion:** 2026-07-13
**Estado inicial:** NUEVO
**Prioridad:** media

## Texto Original

"También los datos de cobro no se ven bien en la grilla." (los encabezados salían truncados "Cli", "M<", "Fo" y el monto cortado a "1.0").

## Objetivo Funcional

Que la grilla "Cobros de la planilla" muestre los datos legibles: encabezados completos, monto sin cortarse y fecha visible. El problema era que la grilla vivía en un layout de 2 columnas fijas (1fr 1fr), quedando a media pantalla y apretando las columnas.

## Criterios De Aceptacion

- [x] El layout de los paneles (Registrar cobro | Cobros de la planilla) es responsive: se apilan en pantallas angostas para que la grilla use el ancho completo.
- [x] La grilla de cobros no trunca encabezados ni el monto (anchos + white-space:nowrap donde corresponde).
- [x] La grilla tiene scroll horizontal como respaldo en pantallas muy angostas (no rompe el layout de la pagina).
- [x] Se agrega la columna Fecha del cobro (dd/MM/yyyy), util ademas para identificar el cobro del dia (REQ-0079).
- [x] Sin cambios de datos ni de backend; solo presentacion.

## Dependencias

- Depende de: REQ-0022/0023 (caja y cobros).
- Relacionado: REQ-0079 (misma grilla).
