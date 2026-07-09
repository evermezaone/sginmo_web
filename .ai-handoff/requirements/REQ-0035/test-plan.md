# Plan de Pruebas - REQ-0035 (F3 motor + SQL nativo)

## Metodo
V27 depende de V26 (columnas renombradas), que aun no esta en la BD viva. Se verifica en una
transaccion: BEGIN + V26 + V27 + prueba funcional del motor + ROLLBACK, contra la BD real.

## Evidencia (EXIT 0, ROLLBACK)
| Test | Esperado | Obtenido |
|---|---|---|
| f_siguiente_numero(1,'REC','T1') | 1 | 1 |
| f_cobrar_documento (70000 de 100000) | cobro con tenant=1; saldo_doc=30000; caja=70000 | cobro tenant=1, saldo=30000, caja=70000 |
| f_anular_cobro motivo inexistente | rechazo | "El motivo NO_EXISTE_XX no existe en MOTIVOS_ANULACION" |
| f_anular_cobro motivo valido (ERROR_CARGA) | anulacion tenant=1, motivo por id; saldo repuesto=100000; caja=0 | anulacion tenant=1 motivo_id=46; saldo=100000; caja=0 |
| WAR completo | mvn package EXIT 0 | EXIT 0 |
| SQL nativo con columnas renombradas | 0 | 0 (grep) |

## Pendiente (fuera de F3)
- Aplicacion real: V26+V27+F2 juntos en el deploy (Codex/ops).
- Aislamiento por tenant en los services (filtros de lectura tenant IN(-1,:t) / =:t, pertenencia
  por id en escrituras): F4 (REQ-0036).
