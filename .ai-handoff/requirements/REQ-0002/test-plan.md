# REQ-0002 - Plan De Pruebas

**Fecha:** 2026-07-04

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn.cmd -q package` | EXIT:0 | **OK** — EXIT:0 |
| T02 | Surefire `DominioBaseTest` | 12 tests sin fallos | **OK** — `Tests run: 12, Failures: 0, Errors: 0, Skipped: 0` |
| T03 | Enums vs especificacion doc 07 §3 | nombres y cantidades exactas (2/2/2/3/4 estados; 2/2/2/2/6/9/2/3/6/4/2/2/2/2/3 tipos) | **OK** — cubierto por asserts de T02 |
| T04 | Listener sin contenedor CDI | crea con usuario "sistema" + fecha; modifica idem; no rompe | **OK** — test `listenerPueblaCreacionYModificacionConFallbackSistema` |

## Pruebas Manuales

| ID | Escenario | Pasos | Resultado esperado | Resultado real |
|---|---|---|---|---|
| M01 | (diferido a REQ-0003) auditoria sobre entidad persistida real | alta de un registro via app | usuario/fecha creacion poblados | pendiente — no hay entidades persistentes hasta REQ-0003 (diferido formal, no es feature de este REQ) |

## Datos De Prueba

No aplica (sin BD en este REQ).
