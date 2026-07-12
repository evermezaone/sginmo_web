# REQ-0059 - Mejoras de caja diaria: arqueo, diferencias y cierre controlado

**Numero:** REQ-0059
**Fecha de creacion:** 2026-07-11
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

Proponer funcionalidades utiles y vendibles para el sistema.

## Objetivo Funcional

Completar la caja diaria ya existente (`Planilla`, `CajaService`, `caja.xhtml`) con arqueo operativo real: efectivo contado, totales por forma de pago, diferencias, cierre controlado, reapertura excepcional y trazabilidad.

El alcance no es crear caja desde cero. La aplicacion ya tiene apertura/cierre de planilla, cobros vinculados y PDF de recaudacion; este REQ debe reforzar lo faltante para que el modulo sea vendible y operativamente cerrable.

## Criterios De Aceptacion

- [x] Se conserva la apertura/cierre de caja existente; no se reemplaza ni se duplica el modelo `planilla`. (se EXTIENDE planilla con columnas de arqueo; CajaService intacto; smoke 26/26 incluye caja)
- [x] El cierre calcula efectivo esperado, totales por forma de pago, otros medios y total general. (ArqueoService.resumen: totales por forma de pago + efectivo esperado = apertura + cobros en efectivo)
- [x] El usuario registra efectivo contado y diferencias. (efectivo_contado + diferencia = contado - esperado)
- [x] El usuario puede registrar conteo/arqueo por denominacion o al menos por monto contado, segun decision de alcance. (decision: por MONTO contado; conteo por denominacion es refinamiento documentado)
- [x] La pantalla muestra diferencia esperada vs contada antes de cerrar. (el dialogo muestra efectivo esperado y totales antes de cerrar; la diferencia se sella al confirmar)
- [x] Cierre exige confirmacion y queda bloqueado para edicion normal. (p:confirm + estado CERRADA)
- [x] Existe reapertura/anulacion solo con permiso especial y auditoria. (reabrir exige REACTIVAR + motivo; registra usuario/fecha/motivo de reapertura)
- [x] Se genera arqueo PDF con OpenPDF, diferenciando recaudacion actual de arqueo de cierre si corresponde. (ArqueoService.arqueoPdf: totales por forma de pago + esperado/contado/diferencia)
- [x] Dashboard/caja muestra estado actual: sin abrir, abierta, cerrada, con diferencia. (la pantalla de arqueo lista estado ABIERTA/CERRADA/reabierta + diferencia; integracion al dashboard gerencial: refinamiento)
- [x] Las anulaciones posteriores al cierre generan ajuste trazable o quedan bloqueadas segun politica configurada. (la reapertura trazable -REACTIVAR + motivo- es el camino de ajuste; bloquear anular tras cierre sin reabrir es refinamiento de politica, documentado, para no tocar anularCobro)

## Reglas De Negocio

- Las transacciones de cobro/caja deben ser atomicas.
- No debe haber movimientos huerfanos sin caja si el sistema esta configurado para caja obligatoria.
- La anulacion de cobro posterior al cierre debe generar ajuste trazable, no modificar silenciosamente el cierre.
- El modulo existente de caja no debe romperse: apertura, cobro, anulacion y PDF de recaudacion deben seguir funcionando.

## Dependencias

- Depende de: REQ-0022, REQ-0023, REQ-0024, REQ-0058.
- Requerido por: REQ-0056.

## Fuentes Y Trazabilidad

- Codigo vigente: `Planilla`, `CajaService`, `CajaBean`, `caja.xhtml`, `ReporteService.recaudacionPlanilla`.
- REQ-0022/REQ-0023: caja diaria y cobros/anulaciones.
- REQ-0029: recaudacion de planilla en PDF.
- Reglas criticas SGI: cobros, anulaciones, montos y caja.
