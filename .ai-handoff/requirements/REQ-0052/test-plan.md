# REQ-0052 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | Build OK | OK |
| T02 | V33 en `BEGIN...ROLLBACK` contra BD real | crea tabla + RLS + pantalla | OK |
| T03 | Dedup: 2 INSERT mismo (tenant,tipo,origen) con ON CONFLICT | queda 1 fila | OK (dedup=1) |
| T04 | Deploy + Flyway V33 en prod | success=t; pantalla `agenda` registrada | OK |
| T05 | `python tools/smoke-test-vps.py` (1er intento) | render OK | agenda dio HTTP 500 (bug) |
| T06 | Diagnostico del 500 | causa raiz identificada | `Map.of` inmutable + clave null en `listar` -> NPE |
| T07 | Fix + rebuild + redeploy + smoke | 20/20 render OK | OK (RESULTADO: TODAS OK) |

## Pruebas Manuales

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | Crear/cerrar tarea | alta y cierre OK | pendiente (verificacion visual del usuario) |
| M02 | Generar automaticos y reabrir | vencimientos sin duplicar | pendiente (requiere datos con vencimientos) |
| M03 | KPIs del tablero | muestran proximos vencimientos y tareas atrasadas | pendiente (verificacion visual) |

## Revision Transversal

- Tabla nueva de negocio con tenant: se creo su RLS inline (V28 no cubre tablas futuras), copiando el
  patron `documento_generado` de V29. Busqueda: `grep "FORCE ROW LEVEL SECURITY" db/migration` -> V28
  (20 tablas) + V29; se aplico el mismo patron a `agenda_evento`.
- `Map.of(...).getOrDefault(null, ...)`: el mismo patron ORDEN existe en `ActivoService`. AgendaService
  ahora guarda el null; los beans de Persona/Operacion pasan orden no-null desde su LazyDataModel, por
  eso no fallaban. Documentado para revisar hermanos si alguno pasara orden null.
- INSERT nativo con auditoria: se setean `usuario_creacion='sistema'`/`fecha_creacion=now()` en el SQL
  (el AuditoriaListener JPA no corre en native queries).

## Datos De Prueba

Tabla vacia al desplegar. Los automaticos requieren cuotas PENDIENTE o contratos con fecha_fin_contrato
proxima en una empresa real.
