# REQ-0051 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package -DbuildCommit=5a658f5` | Build OK | OK |
| T02 | Filtrado de `build-info.properties` | version/fecha/commit sustituidos | OK (0.1.0-SNAPSHOT / 2026-07-12T01:26:31Z / 5a658f5) |
| T03 | V32 en `BEGIN...ROLLBACK` contra BD real | inserta 1 fila PANTALLAS/salud, rollback limpio | OK |
| T04 | Deploy + Flyway V32 en prod | success=t; pantalla `salud` sembrada | OK |
| T05 | `python tools/smoke-test-vps.py` | 19/19 RENDER OK incl. `salud` | OK (RESULTADO: TODAS OK) |
| T06 | Render de `salud` ejercita `snapshot()` | la pagina responde 200 sin error de render (todas las lecturas nativas y de archivo OK) | OK |

## Pruebas Manuales

| ID | Escenario | Pasos | Resultado esperado | Resultado real |
|---|---|---|---|---|
| M01 | Ver panel | Login admin → Configuracion → Salud del sistema | Semaforos por indicador + estado global + sello de version | pendiente (verificacion visual del usuario) |
| M02 | BD caida | Detener PostgreSQL de prueba y refrescar | Indicador Base de datos = CRITICO, pagina no rompe | pendiente (entorno de prueba) |

## Revision Transversal

- Tabla/objeto tocado: catalogo de pantallas `entidad` (lista='PANTALLAS'). Patron de siembra con
  `set_config('app.tenant','-1',true)` identico a V30/V31 (post-RLS). Busqueda: `grep PANTALLAS` en
  migraciones → V14 (patron viejo pre-RLS) y V30/V31 (patron nuevo); se aplico el patron nuevo.
- Menu: se agrego un `h:outputLink` con el mismo patron `rendered=puede('pantalla','VER')` que el resto.
- No se modifico ninguna regla de negocio compartida (dinero/estados/cobros): el service es solo lectura.

## Nota De Alcance (regla de negocio diferida)

"Las alertas criticas deben quedar registradas como evento operativo": diferido. Hoy no existe canal
de evento operativo; se integrara con REQ-0067 (auditoria funcional visible). El panel ya calcula y
muestra el semaforo critico; solo falta persistir el evento, que depende de infra de REQ-0067.

## Datos De Prueba

Ninguno especial. Manifiesto de backup real generado por REQ-0065 en `~/backups/latest.json`.
