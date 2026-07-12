# REQ-0073 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | Build OK | OK |
| T02 | Flyway V50 | objetivo_gerencial + objetivo_medicion + RLS + pantalla | OK |
| T03 | Deploy + smoke | 34/34 incl. objetivos | OK |
| T04 | Semaforo MINIMO/MAXIMO | OK/ADVERTENCIA/CRITICO segun meta/umbral | OK (por codigo) |
| T05 | Validacion por unidad | PORCENTAJE 0-100; MONTO exige moneda | OK (por codigo/CHECK) |

## Pruebas Manuales

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | Crear "ocupacion 90%" | semaforo + brecha + "faltan N prop." | pendiente (verificacion del usuario) |
| M02 | Crear "mora_maxima" en una moneda | semaforo rojo si supera la meta | pendiente |
| M03 | Registrar medicion | queda en historial (objetivo_medicion) | pendiente |
| M04 | Inactivar/reactivar (permiso) | baja logica + auditoria | pendiente |
| M05 | Dos empresas | cada una ve solo sus objetivos (RLS) | pendiente |

## Datos De Prueba

Un usuario con permiso objetivos/CREAR y datos de ocupacion/cobros para ver el calculo.

## Nota

El calculo automatico reutiliza REQ-0069/0071/0072; el drill-down y el dashboard (0074/0070) lo consumen.
