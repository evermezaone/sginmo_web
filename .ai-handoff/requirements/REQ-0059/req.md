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

- [ ] Se conserva la apertura/cierre de caja existente; no se reemplaza ni se duplica el modelo `planilla`.
- [ ] El cierre calcula efectivo esperado, totales por forma de pago, otros medios y total general.
- [ ] El usuario registra efectivo contado y diferencias.
- [ ] El usuario puede registrar conteo/arqueo por denominacion o al menos por monto contado, segun decision de alcance.
- [ ] La pantalla muestra diferencia esperada vs contada antes de cerrar.
- [ ] Cierre exige confirmacion y queda bloqueado para edicion normal.
- [ ] Existe reapertura/anulacion solo con permiso especial y auditoria.
- [ ] Se genera arqueo PDF con OpenPDF, diferenciando recaudacion actual de arqueo de cierre si corresponde.
- [ ] Dashboard/caja muestra estado actual: sin abrir, abierta, cerrada, con diferencia.
- [ ] Las anulaciones posteriores al cierre generan ajuste trazable o quedan bloqueadas segun politica configurada.

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
