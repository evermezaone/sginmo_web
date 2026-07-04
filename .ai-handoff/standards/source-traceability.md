# Estandar De Trazabilidad De Reglas — SGInmo Web

Toda regla de negocio crítica implementada debe citar una fuente de verdad verificable. Sin fuente, la regla es una suposición y Codex debe observarla.

## Fuentes válidas (en orden de preferencia)

1. **ID de regla documentada**: `RN-*` de `docs-migracion/03-reglas-negocio-nucleo.md` o sección del doc 04 (servicios).
2. **Código legado**: archivo + método de `Pysistemas\Inmobiliaria\` (ej.: `FrmCobros.cs → CalcularMontoPagar()`, `CronogramasCuotasService.cs → GenerarCuotas()`).
3. **Objeto de BD legada**: SP de Firebird (`RPT_PLANILLA_RECAUDACIONES`, `SP_AUX_COBRO_CUOTA`) o valor real de `DOMINIOS` (doc 07 §2-3).
4. **Decisión explícita del usuario**: registrada en `user-decision.md` del REQ o en el chat de coordinación (citar fecha).

## Dónde citar

- Javadoc del método de servicio que implementa la regla.
- `req.md` (criterios de aceptación) y `analysis.md` del REQ.
- La consulta de cada JasperReport cita el SP `RPT_*` fuente.

## Desvíos deliberados del legado

Los 7 bugs del legado listados en `CLAUDE.md` (transacciones, cuotas duplicadas, redondeo, moneda fija, passwords, autorización UI-only, estado de propiedad desincronizado) se corrigen SIEMPRE. Cada desvío adicional respecto del comportamiento documentado requiere: justificación en `analysis.md` + decisión del usuario si cambia semántica de negocio.

## Regla anti-invención

Si el legado no cubre un caso (ej.: módulos sin datos reales como VENTA/liquidaciones) y la doc no lo especifica, NO inventar comportamiento: plantear la pregunta concreta y derivar a `ESPERA_USUARIO`/`BLOQUEADO_POR_USUARIO` con la duda registrada.
