# REQ-0075 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | Build OK | OK |
| T02 | Flyway V51 | alerta_gerencial + RLS + param + pantalla | OK |
| T03 | Deploy + smoke | 36/36 incl. alertas | OK |
| T04 | Dedup | unique parcial ABIERTA + chequeo upsert | OK (por codigo/DDL) |
| T05 | Descartar sin motivo | rechazo | OK (por codigo) |

## Pruebas Manuales

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | Objetivo no cumplido -> Recalcular | alerta con causa/impacto/accion + evidencia | pendiente (verificacion del usuario) |
| M02 | Descartar con motivo | auditada; no reaparece (dedup) | pendiente |
| M03 | Rentabilidad negativa | alerta CRITICA | pendiente |
| M04 | Contratos por vencer (N dias) | alerta MEDIA | pendiente |
| M05 | Dos empresas | RLS aisla | pendiente |

## Datos De Prueba

Objetivos activos (REQ-0073) y datos de ocupacion/cobros/operaciones.

## Nota

Preparado para envio por email/notificacion (modelo completo), no implementado en v1.
