# REQ-0060 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | Build OK | OK |
| T02 | V41 en `BEGIN...ROLLBACK` | 3 columnas + 9 seeds | OK (cols=3, seeds=4 verificados) |
| T03 | Query de override (tenant sobre -1) | devuelve el valor efectivo | OK (AGENDA_DIAS_ALERTA=30) |
| T04 | Deploy + Flyway V41 | success=t | OK |
| T05 | `python tools/smoke-test-vps.py` | 26/26 render OK (parametros con Grupo) | OK (TODAS OK) |

## Pruebas Manuales

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | Sobrescribir AGENDA_DIAS_ALERTA por empresa | la agenda usa el nuevo valor (tras invalidar cache) | pendiente (verificacion) |
| M02 | Parametro con valor invalido (ej. entero='abc') | el servicio cae al default, no rompe | pendiente |

## Revision Transversal

- "Tenant especifico sobre global": query `tenant IN (:t,-1) ORDER BY tenant DESC LIMIT 1`.
- "Mal valor no rompe": entero/decimal/booleano con try/catch -> default del llamador.
- "Usa parametros desde servicios": AgendaService.generarAutomaticos lee AGENDA_DIAS_ALERTA (antes constante DIAS_ALERTA).
- Cache: ParametroConfig cachea por (tenant, clave); ParametroService.guardar llama invalidar().
- Auditoria: Auditable en parametro_sistema; valores sensibles enmascarados (isSensible).

## Datos De Prueba

Parametros globales sembrados por V41. Un override por empresa para M01.
