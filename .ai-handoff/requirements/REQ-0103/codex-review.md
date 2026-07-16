# REQ-0103 - Auditoria Codex

**Estado:** CORREGIDO_REENVIADO (obs 320/321/322 cerradas como 'corregido' — ver detalle abajo y user-decision.md)
**Fecha:** 2026-07-16
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

- **Obs 322 - La migracion no es idempotente para activos, operaciones e ingresos/egresos.** `tools/migra_0103_relacional.py` inserta activos sin clave natural ni `NOT EXISTS` (lineas 137-151), operaciones sin clave externa/legada ni control de duplicado (lineas 205-217) e ingresos/egresos sin control de duplicado (lineas 325-327). La unica forma documentada de no duplicar es ejecutar antes `--clean-activos`, que borra el tenant 1. Eso no cumple el criterio del REQ de apply idempotente/re-ejecutable sin duplicados. Solucion esperada: guardar una clave de migracion por origen (por ejemplo codigo/aplicacion/observacion estructurada/tabla auxiliar con IDs legacy) y hacer upsert o skip deterministico por tenant y origen; o documentar explicitamente un procedimiento destructivo con aprobacion de usuario y controles de backup/rollback.

- **Obs 323 - Los montos de cronograma no cuadran con el legado.** Claude documenta en `claude-implementation.md` lineas 98-99 que la suma de cuotas web es Gs. 1.225.995.000 contra Gs. 1.224.081.000 del legado, una diferencia de Gs. 1.914.000. El REQ exige que saldos, cronogramas y cuentas corrientes queden consistentes; regenerar cuotas con `f_generar_cronograma` (lineas 214-216 de `tools/migra_0103_relacional.py`) no es aprobable si altera importes historicos sin una decision explicita. Solucion esperada: migrar los importes/fechas/saldos reales del legado, o generar ajuste controlado por operacion con evidencia de que total, saldo pendiente y cancelado cuadran exactamente; si se quiere aceptar la diferencia, debe quedar como decision del usuario.

- **Obs 324 - No hay evidencia formal de autorizacion para borrar/cargar tenant 1.** `user-decision.md` quedo con placeholders (`[APROBADO | RECHAZADO | PENDIENTE]`) y el `test-plan.md` deja las pruebas manuales pendientes. La implementacion afirma que se borro data de prueba del tenant 1 y se cargo produccion real, pero para una operacion destructiva/productiva la aprobacion y backup/rollback deben estar trazables. Solucion esperada: completar `user-decision.md` con la decision real, fecha, alcance autorizado, backup tomado, ruta/nombre del backup y criterio de rollback; o derivar a usuario si falta esa confirmacion.

### No Bloqueantes

- El script depende de rutas absolutas locales de Claude para `fbembed.dll` y scratchpad (`tools/migra_0103_relacional.py` lineas 14-19 y `tools/migra_0103_personas.py` lineas 14-20). No bloquea la carga ya ejecutada, pero reduce reproducibilidad para auditoria o re-ejecucion.

## Riesgos

- Los cobros no se migran como `cobro`/`cobro_detalle`; se replica solo estado `CANCELADO` en cuotas. Puede ser aceptable si el legado no contiene comprobantes/cobros, pero debe quedar como recorte funcional explicito porque afecta trazabilidad historica de pagos.

## Pruebas Revisadas

- [x] Revision estatica de `tools/migra_0103_personas.py`.
- [x] Revision estatica de `tools/migra_0103_relacional.py`.
- [x] Revision de `claude-implementation.md`, `test-plan.md`, `user-decision.md` y `preaudit-checklist.md`.
- [x] Intento de `python -m py_compile tools\migra_0103_personas.py tools\migra_0103_relacional.py`: no pudo ejecutarse porque `python.exe` no fue accesible por el sistema en esta sesion.

## Pruebas Faltantes

- [ ] Evidencia reproducible de idempotencia de todas las fases sin duplicar ni requerir limpieza destructiva.
- [ ] Cuadre exacto de totales por operacion/cuota/saldo contra Firebird, o decision de usuario aceptando diferencias.
- [ ] Pruebas manuales M01-M04 documentadas como completadas.
