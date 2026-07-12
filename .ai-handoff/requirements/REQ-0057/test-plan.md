# REQ-0057 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | Build OK | OK |
| T02 | V38 en `BEGIN...ROLLBACK` | 2 tablas + 8 RLS + pantalla + insert promesa | OK |
| T03 | Backup previo | dump OK | OK |
| T04 | Deploy + Flyway V38 | success=t | OK |
| T05 | `python tools/smoke-test-vps.py` | 24/24 render OK incl. cobranza | OK (TODAS OK) |

## Pruebas Manuales

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | Cartera con cuotas vencidas | lista con dias/saldo/mora; filtros | pendiente (requiere cuotas vencidas) |
| M02 | Registrar gestion + promesa | quedan en el historial | pendiente |
| M03 | Promesa vencida | aparece evento PROMESA en la Agenda | pendiente |
| M04 | Export CSV | descarga la cartera (permiso EXPORTAR) | pendiente |

## Revision Transversal

- Mora: se usa `f_mora_cuota(cuota, current_date)` (misma funcion que el modulo de cobros); dias =
  current_date - fecha_vencimiento. No se duplica calculo divergente.
- No se modifica cronograma_cuota desde cobranza (cartera es solo lectura; gestiones/promesas aparte).
- Promesa != pago: promesa_pago no toca cronograma_cuota ni cobro.
- RLS por tenant en ambas tablas (patron V29). Escrituras exigen permiso EDITAR.
- Integracion agenda: se agrego un INSERT en AgendaService.generarAutomaticos con dedup por origen;
  se reviso que respeta el indice unico parcial ya existente (REQ-0052).

## Datos De Prueba

Empresa con cuotas PENDIENTE vencidas. Una promesa con fecha pasada para ver el evento en agenda.
