# 03 — Reglas de negocio del núcleo (extraídas de los formularios)

En este sistema gran parte de las reglas vive en los event handlers de los forms WinForms. Este doc las cataloga con ID propio (RN-*) y referencia a archivo. En la migración **todas deben moverse a la capa de servicios/dominio del backend**.

## 1. Cobros (`Presentacion\Cobros\Cobros\FrmCobros.cs`)

Flujo: buscar cliente (por razón social o documento) → listar operaciones/cuotas pendientes → seleccionar cuotas → calcular mora → elegir forma de pago → grabar cabecera + detalles → opcional descuento → opcional anulación.

- **RN-COBR-001 Cálculo de mora** (`CalcularMontoPagar()`, y `CobrosService.CalcularMora`):
  `si fecha_cobro > fecha_vencimiento + dias_gracia → mora = dias_excedidos × MONTO_MORA (por día, de OPERACIONES_PROPIEDADES)`. Monto a pagar = cuota + mora.
- **RN-COBR-002 Detalles del cobro**: por cada cuota se generan hasta 2 detalles: tipo `CUOTA` (monto cuota) y tipo `MORA` (si aplica). Valores fijos hoy: MONEDA_DOCUMENTO=1, MONEDA_PAGO=1, TASA_CAMBIO=1, CONDICION="CONTADO".
- **RN-COBR-003 Validaciones**: forma de pago obligatoria (default "DB"); al menos una cuota seleccionada; monto total > 0.
- **RN-COBR-005 Señalización**: cuotas vencidas a la fecha se pintan en rojo con aviso de días de mora.
- **RN-COBR-006 Descuento (solo ADMINISTRADOR)**: agrega detalle tipo `DESCUENTO` con monto negativo, FORMA_PAGO_ID=8 (hardcodeado), usando el ítem "DESCUENTO" de ITEMS_INGRESOS_EGRESOS. Modal `FrmAgregarDescuento` (solo números, formato N0).
- **RN-COBR-007 Anulación (solo ADMINISTRADOR)**: precondición estado cobro = CANCELADO. Por cada detalle: la cuota vuelve a `PENDIENTE` y se limpia FECHA_CANCELACION; el cobro pasa a `ANULADO`.
- **Numeración de comprobante**: entrada "1-1-123" se autoformatea a "001-001-0000123".

## 2. Operaciones / contratos (`FrmOperaciones.cs`)

Flujo NUEVO: elegir tipo operación → entidad o propiedad → locatario (socio) → condición/plazo/precio/garantía → generar cronograma → grabar operación + movimientos automáticos (comisión, garantía).

- **RN-OPE-001 Monto total**: VENTA → total = precio; ALQUILER → total = precio × plazo. Se genera también el monto en letras (`Convertir_Numero`).
- **RN-OPE-002 Comisión**: ALQUILER → comisión = garantía × %comisión/100; VENTA → comisión = precio × %comisión/100 (el % viene de PROPIEDADES.COMISION, con default en dominios PORCENTAJE_COMISION_*).
- **RN-OPE-012 Egreso automático por comisión** (solo NUEVO, no renovación): crea INGRESOS_EGRESOS tipo EGRESO, ítem `COMISION_VENTA`/`COMISION_ALQUILER`, estado CANCELADO, forma de pago "EFE", asociado al vendedor y la propiedad.
- **RN-OPE-003/013 Garantía (solo ALQUILER)**: garantía se precarga = precio; al grabar se crea INGRESOS_EGRESOS tipo INGRESO ítem `DEPOSITO_GARANTIA`, estado CANCELADO. En VENTA el campo garantía se deshabilita.
- **RN-OPE-004 Validaciones obligatorias**: tipo de operación, entidad o propiedad (según radio), locatario, tipo de contrato, condición, plazo > 0.
- **RN-OPE-007 Condición**: CONTADO → plazo = 1 cuota; CREDITO → plazo manual.
- **RN-OPE-008 Fechas**: fin contrato = inicio + plazo meses (recalcula al cambiar inicio o plazo).
- **RN-OPE-009 Renovación**: si AVISO_RENOVACION=SI → FECHA_RENOVACION = fin − 30 días.
- **RN-OPE-010 Precarga al elegir propiedad**: precio desde PROPIEDADES_VIEW; en alquiler garantía=precio, en venta garantía=0; muestra características y ubicación.
- **RN-OPE-011 Filtro de propiedades**: en NUEVO solo propiedades estado LIBRE (de la entidad elegida o sin entidad); en EDITAR todas.
- Estado inicial de la operación: **VIGENTE**. Mora y días de gracia se toman de dominios `MONTO_MORA_DEFECTO` y `DIAS_GRACIA_VENCIMIENTO` como default, editables por operación.

## 3. Cronograma de cuotas (`FrmVerCronogramasCuotasOperaciones.cs`, `FrmEditarCuotas.cs`)

- **RN-CUO-001 Generación** (via `CronogramasCuotasService.GenerarCuotas`): N=plazo cuotas, estado PENDIENTE, vencimientos mensuales desde inicio de contrato (detalle de la fórmula en doc 04 §2).
- **RN-CUO-002 Redondeo**: si la suma de cuotas ≠ precio (por división), la diferencia se ajusta en la **última cuota**.
- **RN-CUO-003 Validación**: en VENTA la suma de cuotas debe = precio (error si no); en ALQUILER no se valida.
- **RN-CUO-004** El total se recalcula en vivo al editar montos de cuotas en la grilla.
- **FrmEditarCuotas**: permite modificar monto y fecha de vencimiento de una cuota individual (con auditoría).

## 4. Liquidaciones (`FrmLiquidaciones.cs`, `FrmPlantillaLiquidacion.cs`)

Cierre de un alquiler: se liquida la garantía contra los gastos.

- **RN-LIQ-001 Saldo = garantía − total gastos**; si < 0 se muestra en rojo (deuda del inquilino).
- **RN-LIQ-003/004 Validaciones**: operación seleccionada y motivo de liquidación (dominio MOTIVO_LIQUIDACion) obligatorios.
- **Plantilla de gastos** (`FrmPlantillaLiquidacion`): categorías precargadas — alquileres pendientes, cerrajería, materiales, mano de obra, ANDE, administrativos, limpieza, mora. Por cada categoría con monto > 0 se crea un detalle con su ítem de ITEMS_INGRESOS_EGRESOS (mapeo por campo APLICACION).
- **RN-PLANT-001/002**: los "alquileres pendientes" = suma de cuotas PENDIENTES de la operación; la "mora" = mora calculada de esas cuotas a la fecha de liquidación (misma fórmula de RN-COBR-001).
- Al guardar la liquidación (ver doc 04 §3): la operación pasa a **FINALIZADO** y la propiedad a **LIBRE**.

## 5. Renovación de contratos (`FrmRenovacionContratos.cs`)

Grid alimentado por la vista OPERACIONES_CONTRATOS_VENCIDOS.

- **RN-REN-001/002**: no se puede renovar con cuotas PENDIENTES (bloqueo con mensaje).
- **RN-REN-003 Proceso**: abre FrmOperaciones en modo NUEVO con bandera EsRenovacion, precargando datos del contrato anterior (fechas = fin anterior + 1 día; entidad/propiedad bloqueadas). Si el usuario guarda: contrato anterior pasa a **FINALIZADO** con FECHA_RENOVACION = hoy. Alternativa desde la misma pantalla: finalizar el contrato → abre FrmLiquidaciones.
- ⚠️ Bug conocido: la rama "R" del servicio no elimina las cuotas del contrato anterior (ver doc 04).

## 6. Rescisiones (`FrmRescisiones.cs`)

Cancelación anticipada: registra RESCISIONES (fecha, tipo, monto de penalidad, observación) contra la operación.

## 7. Propiedades, lotes y entidades (`FrmPropiedades.cs`, `FrmLotes.cs`, `FrmEntidadesInmobiliarias.cs`, `FrmGenerarLotes.cs`)

- **RN-PROP-001**: FrmPropiedades trabaja con entidades tipo ≠ LOTEAMIENTO; FrmLotes solo con LOTEAMIENTO y tipo de propiedad fijo LOTE.
- **RN-PROP-002 / RN-LOT-003**: % de comisión default desde dominios (`PORCENTAJE_COMISION_ALQUILER` / `PORCENTAJE_COMISION_VENTA`).
- **RN-PROP-003**: estado LIBRE se muestra verde, OCUPADO rojo.
- **RN-ENT-001**: unicidad de nombre de entidad por tipo (validación case-insensitive al salir del campo).
- **FrmGenerarLotes** (proceso masivo): elegir loteamiento + manzana + rango numérico de lotes → genera N propiedades tipo LOTE.
- **FrmRegenerarCuotas** (proceso, solo ADMINISTRADOR): si cambia el plazo de una operación → recalcula FECHA_FIN_CONTRATO y regenera el cronograma completo.

## 8. Máquinas de estado consolidadas

```
OPERACIÓN:   (alta) → VIGENTE → FINALIZADO   [renovación, liquidación]
                          └──→ (RESCISIONES registra la rescisión)
CUOTA:       (alta) → PENDIENTE ⇄ CANCELADO  [cobro / anulación de cobro]
COBRO:       (alta) → CANCELADO → ANULADO    [solo ADMINISTRADOR; revierte cuotas]
PROPIEDAD:   LIBRE → OCUPADA (alquiler) | VENDIDA (venta) → LIBRE (liquidación)
```

## 9. Valores hardcodeados a parametrizar en la migración

| Valor | Significado | Ubicación |
|---|---|---|
| FORMA_PAGO_ID = 8 | forma de pago de descuentos | FrmCobros |
| "EFE" / "DB" | formas de pago default | FrmOperaciones / FrmCobros |
| MONEDA = 1, TASA = 1 | guaraní fijo | FrmCobros, CronogramasCuotasService |
| 30 días | anticipación de aviso de renovación | FrmOperaciones |
| CONDICION = "CONTADO" | condición fija de cobros | FrmCobros |
| Estados como strings | "VIGENTE", "PENDIENTE", "CANCELADO", "ANULADO", "FINALIZADO", "LIBRE"… | en todos los forms — convertir a enums/constantes |
