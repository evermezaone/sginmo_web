# Plan de Pruebas - REQ-0037 (F5 RLS)

## Metodo
V28 depende de V26/V27 (no aplicadas a la BD viva). Se verifica en una transaccion:
BEGIN + V26 + V27 + V28 + prueba de aislamiento con 2 tenants + ROLLBACK, contra la BD real.

## Evidencia (EXIT 0, ROLLBACK)
| Test | Esperado | Obtenido |
|---|---|---|
| rol app sginmo | no superuser / no bypassrls | rolsuper=f, rolbypassrls=f |
| tenant 1 lee activos | solo los suyos (no ve tenant 500) | solo tenant-1 (2 filas; una pre-existente) |
| tenant 500 lee activos | solo el suyo | 1 |
| SUPERADMIN lee activos | todos | 3 |
| tenant 1 INSERT activo de tenant 500 | negado (WITH CHECK) | RLS nego el insert |
| sin app.tenant lee activos | 0 (fail-closed) | 0 |
| catalogo (entidad) tenant 1 | ve globales -1 | 108 |
| WAR con interceptor | mvn package EXIT 0 | EXIT 0 |

## Pendiente (fuera de F5)
- Validacion runtime del interceptor (app corriendo, sin fail-closed indebido): F7 (REQ-0039),
  tras aplicar V26+V27+V28.
