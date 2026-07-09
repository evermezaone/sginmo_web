# Plan de Pruebas - REQ-0039 (F7)

## Metodo
Bateria SQL con 2 empresas corrida sobre V26+V27+V28 en BEGIN...ROLLBACK contra la BD REAL,
como el rol de la app (sginmo, FORCE RLS). La BD viva no se toca.

## Evidencia (EXIT 0, ROLLBACK)
| Assert | Esperado | Resultado |
|---|---|---|
| A1 SUPERADMIN (app.tenant=-1) ve ambas sucursales | 2 | OK |
| A2 Empresa A ve su sucursal, no la de B | 1 / 0 | OK |
| A2 Empresa A ve catalogo global+propio, no el de B | 2, sin 'B' | OK |
| A3 Empresa B aislada de A | 1 / 0, sin 'A' | OK |
| A4 INSERT cross-tenant | NEGADO por RLS | OK (excepcion capturada) |
| A5 UPDATE cross-tenant | 0 filas afectadas | OK |
| A6 "operar como" acota al tenant elegido | 1 | OK |
| rol app sginmo | no superuser / no bypassrls | rolsuper=f, rolbypassrls=f |
| BD viva tras la corrida | intacta | flyway V25 (ROLLBACK) |

## Pos-deploy (fuera de esta corrida, tras aplicar la unidad F1-F6)
- Login HTTP real de adminA / adminB / superadmin; cada admin ve/opera solo su empresa;
  el superadmin ve todo y "opera como"; sin dejar datos fuera de las 2 empresas de demo.
