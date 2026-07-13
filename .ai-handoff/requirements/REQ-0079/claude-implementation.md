# REQ-0079 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-13
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0079
- Tipo de cambio: backend (CajaService) + UI (caja.xhtml, CajaBean). Sin BD.
- Riesgo: medio (accion sensible de anulacion de cobro).
- Archivos clave:
  - `servicio/CajaService.java` (anularCobro): ademas del motivo obligatorio, re-valida en backend que el cobro este ACTIVO, sea del dia de HOY y sea el ULTIMO cobro ACTIVO de su planilla (COUNT de cobros ACTIVOS con id mayor = 0). Si no, NegocioException con mensaje claro. Sigue auditando ANULAR con motivo (REQ-0067).
  - `web/CajaBean.java`: `modoAnulacion` (boolean, escondido por defecto) + `alternarModoAnulacion()`; `getCobroAnulableId()` = id del ultimo cobro ACTIVO si es de hoy (cobros vienen id DESC, el primer ACTIVO es el mas reciente); al anular se apaga el modo.
  - `webapp/caja.xhtml`: la grilla ya NO muestra la X por defecto; un boton discreto "Anular ultimo cobro..." (solo con permiso caja/INACTIVAR) habilita el modo, que muestra un aviso rojo + el selector de motivo; la X solo aparece para `cb == cobroAnulableId` en modo anulacion, con confirmacion.
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - Deploy VPS; `python tools/smoke-test-vps.py`: 36/36 (caja 200).
- Cambios de datos: no. Cambios de entorno: no.
- Decision esperada: aprobar; revisar que la validacion de "ultimo cobro del dia" este en backend y no solo en UI.
- Notas para auditor:
  - Defensa en profundidad: la UI esconde/gatea el boton, pero la regla real (hoy + ultimo) se valida en CajaService.anularCobro; saltar la UI no permite anular cobros antiguos ni intermedios.
  - Motivo obligatorio ya existente; se conserva. Auditoria ANULAR con motivo (REQ-0067).
  - Permiso caja/INACTIVAR requerido para ver el control y para anular (server-side exigir()).

## Resumen Funcional

La anulacion de un cobro deja de estar pegada al boton de imprimir. Ahora el usuario debe habilitar un "modo anulacion" con un control aparte; recien ahi aparece, solo en el ultimo cobro del dia, la X para anular (con motivo y confirmacion).

## Resumen Tecnico

UI: modo anulacion escondido + X limitada al ultimo cobro de hoy. Backend: CajaService.anularCobro valida ACTIVO + fecha=hoy + ultimo cobro ACTIVO de la planilla antes de f_anular_cobro.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| servicio/CajaService.java | validacion backend (hoy + ultimo cobro ACTIVO) en anularCobro |
| web/CajaBean.java | modoAnulacion + alternarModoAnulacion + getCobroAnulableId |
| webapp/caja.xhtml | control escondido, motivo condicional, X limitada |

## Cambios De Datos

Sin cambios.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; deploy VPS; smoke 36/36 (caja 200).

## Pruebas Manuales Sugeridas

1. Caja abierta con varios cobros de hoy: por defecto no hay X; habilitar modo -> la X aparece solo en el ultimo cobro; anular sin motivo -> rechaza; con motivo -> anula y repone saldo.
2. Cobro de un dia anterior (o intermedio): aunque se fuerce, el backend rechaza ("solo el ultimo cobro" / "solo un cobro de hoy").
3. Usuario sin permiso caja/INACTIVAR: no ve el control ni la X.

## Limitaciones Conocidas

- Se opto por un boton "escondido" (mas claro y accesible) en lugar de una combinacion de teclas.

## Riesgos Conocidos

- Accion sensible: mitigada con validacion backend, motivo obligatorio, confirmacion, permiso y auditoria.
