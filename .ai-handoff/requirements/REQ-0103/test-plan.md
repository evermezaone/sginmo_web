# REQ-0103 - Plan De Pruebas

**Fecha:** 2026-07-16

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | Lectura del legado (fbembed + fdb) y conteos | Reporta esquema/conteos reales | OK: 69 socios, 68 activos, 44 operaciones, 459 cuotas, 56 ing/egr |
| T02 | `--clean-activos` borra data de prueba tenant 1 en orden FK | Sin violacion de FK | OK (savepoints por sentencia) |
| T03 | `--activos` (entidades+propiedades) | 7 + 61 = 68 en `activo` tenant 1 | OK |
| T04 | `--operaciones` genera cronograma via f_generar_cronograma | 44 operaciones, 459 cuotas | OK |
| T05 | `--marcar-cuotas` copia estado de pago del legado | 229 cuotas CANCELADO | OK |
| T06 | `--propietarios` enlaza por entidad | 7 en `activo_propietario` | OK |
| T07 | `--ingresos` crea articulos + egresos | 23 articulos + 56 ing/egr | OK |
| T08 | Idempotencia personas (re-run por numero_documento) | No duplica | OK |
| T09 | Comparacion legado vs web (conteos) | Todos coinciden | OK |

## Pruebas Manuales

| ID | Escenario | Pasos | Resultado esperado | Resultado real |
|---|---|---|---|---|
| M01 | Listado de propiedades en el web | Login Pysistemas > Propiedades | 68 activos visibles | pendiente (usuario) |
| M02 | Contratos y cronograma | Operaciones > abrir un contrato | Cuotas pagadas figuran canceladas | pendiente (usuario) |
| M03 | Reporte cobranza/mora | Reportes > cobranza | 229 pagadas / 230 pendientes | pendiente (usuario) |
| M04 | Caja de egresos | Ingresos/Egresos | 56 movimientos | pendiente (usuario) |

## Datos De Prueba

Base legada real: `migracion/source/INMOBILIARIA.FDB`. Destino: PostgreSQL VPS (tenant 1 = Pysistemas),
alcanzado por tunel SSH `127.0.0.1:15432`. Backup tomado antes de aplicar.
