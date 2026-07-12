# REQ-0046 - Analisis

**Estado:** EN_ANALISIS
**Fecha:** 2026-07-11
**Analista:** Claude

## Analisis Funcional

El alta de operacion falla siempre porque la columna `moneda` es NOT NULL en la BD pero el
formulario nunca ofrecio un campo para elegirla, por lo que el bean intenta persistir la
operacion con `moneda = null` y la BD rechaza el INSERT por integridad. El flujo de registro
de operaciones queda completamente bloqueado.

Solucion: exponer un selector de Moneda obligatorio en el formulario, alimentado con las
monedas activas visibles al tenant, y preseleccionar Guaranies por defecto al crear una
operacion nueva.

## Analisis Tecnico

- `operaciones.xhtml`: se agrega un `p:selectOneMenu` de Moneda con `required="true"` ligado a
  `#{operacionBean.seleccionado.moneda}` y `f:selectItems` sobre `#{operacionBean.monedas}`
  (itemLabel = descripcion, itemValue = id).
- `OperacionBean.java`: se agrega la lista `monedas` (cargada en el init con
  `catalogoService.monedasActivas()`), su getter, y en `nuevo()` se preselecciona por defecto
  la moneda cuya descripcion contiene "guaran" (Guaranies) o, en su defecto, la primera visible.
- `CatalogoService.java`: nuevo metodo `monedasActivas()` = `SELECT m FROM Moneda m WHERE
  m.estado = 'ACTIVO' AND (m.tenant = -1 OR m.tenant = :t) ORDER BY m.descripcion`. Respeta el
  patron multi-tenant (globales -1 + propias del tenant).
- Usa tablas existentes (catalogo `Moneda`); no requiere migracion.

## Riesgos

| Riesgo | Probabilidad | Impacto | Mitigacion |
|---|---|---|---|
| No existe ninguna Moneda ACTIVA para el tenant | baja | medio | `required` bloquea el guardado con mensaje; sembrar Guaranies como global -1 |
| La descripcion de Guaranies no contiene "guaran" | baja | bajo | fallback a la primera moneda visible (`orElse(monedas.get(0))`) |
| Operaciones historicas sin moneda | baja | bajo | el fix es para altas nuevas; no altera registros existentes |

**Semaforo Codex:** medio

## Preguntas Abiertas

- [ ] Ninguna.

## Impacto En Costos / LLM

- Aumenta tokens por mensaje: no
- Agrega llamadas extra al LLM: no
- Puede resolverse sin IA: si

## Impacto En Datos

- Requiere migracion: no (usa el catalogo Moneda existente)
- Tablas/colecciones afectadas: lectura de `moneda` (catalogo); escritura de `operacion.moneda` en altas

## Recomendacion

**Desarrollar** — bug bloqueante de integridad; riesgo medio acotado a la carga de moneda en el alta.
