# REQ-0104 - Dashboard Inicio: numero KPI grande desborda el recuadro (font adaptable)

**Numero:** REQ-0104
**Fecha de creacion:** 2026-07-16
**Estado inicial:** NUEVO
**Prioridad:** baja (cosmetico)

## Texto Original

"la estetica cuando el nro es muy grade ya no cabe en el recuadro: 1.225.995.000"
(observado en el dashboard de Inicio tras cargar datos reales, tarjeta "Saldo por cobrar (Gs.)").

## Objetivo Funcional

Los KPI numericos del Inicio (Saldo por cobrar, Recaudado hoy, etc.) deben mostrar montos grandes
(miles de millones de Gs.) sin desbordar el recuadro de la tarjeta.

## Criterios De Aceptacion

- [x] Un monto como 1.225.995.000 se muestra completo dentro del recuadro, sin recorte ni desborde.
- [x] Los KPI de numeros chicos (0, 40, 45) conservan su tamano legible (no se achican de mas).
- [x] Sin cambios funcionales ni de datos; solo presentacion.

## Dependencias

- Depende de: ninguna
- Requerido por: ninguno
- Archivo: `Desarrollo/sginmo-web/src/main/webapp/index.xhtml` (CSS de `.kpi .n`).
