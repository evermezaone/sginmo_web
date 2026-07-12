# REQ-0046 - Plan De Pruebas

**Fecha:** 2026-07-11

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package -DskipTests` | Build OK | OK |
| T02 | `python tools/smoke-test-vps.py` | 19/19 RENDER OK incl. `operaciones` | OK (RESULTADO: TODAS OK) |
| T03 | Render de `operaciones` con el combo de Moneda | la pagina responde 200; el `selectOneMenu` de Moneda ejercita `getMonedas()` sin error | OK |
| T04 | `nuevo()` preselecciona moneda | al crear operacion, `seleccionado.moneda` queda seteada a Guaranies (o primera visible) | OK |

## Pruebas Manuales

| ID | Escenario | Pasos | Resultado esperado | Resultado real |
|---|---|---|---|---|
| M01 | Alta con moneda default | Login → Operaciones → Nuevo → completar y Guardar | Se registra la operacion sin error de integridad; moneda = Guaranies | pendiente (verificacion del usuario) |
| M02 | Moneda obligatoria | Vaciar el selector de Moneda y Guardar | Bloquea con mensaje `required`; no intenta persistir null | pendiente (verificacion del usuario) |

## Revision Transversal

- Tabla/objeto tocado: catalogo `Moneda` (solo lectura via `monedasActivas()`) y escritura de
  `operacion.moneda` en altas. El patron `estado='ACTIVO' AND (tenant=-1 OR tenant=:t)` es el
  mismo que `formasHabilitadas()` y demas metodos multi-tenant de `CatalogoService`.
- No se modifico ninguna regla de dinero/estados/cobros compartida: se agrega un campo que
  antes faltaba, corrigiendo el INSERT que fallaba por NOT NULL.

## Nota De Alcance

El fix corrige altas nuevas. Operaciones historicas con moneda inconsistente (si las hubiera)
no se tocan; quedan fuera del alcance de este bug.

## Datos De Prueba

Requiere al menos una Moneda ACTIVA visible al tenant (idealmente Guaranies como global -1).
