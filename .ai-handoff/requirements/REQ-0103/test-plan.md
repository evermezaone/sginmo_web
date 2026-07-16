# REQ-0103 - Plan De Pruebas (flujo exacto: cronograma + documento + cobros)

**Fecha:** 2026-07-16

Herramientas finales: `tools/migra_0103_personas.py`, `tools/migra_0103_relacional.py` (activos/operaciones/
ingresos-egresos), `tools/migra_0103_exacto.py` (cronograma+documento+cobros con importes/fechas/estados
VERBATIM del legado). Destino: PostgreSQL VPS, tenant 1 (Pysistemas), via tunel SSH 127.0.0.1:15432.

## Consultas de cuadre reproducibles (legado vs web)

Legado: Firebird `source/INMOBILIARIA.FDB` (via fdb + fbembed). Web: PostgreSQL, `SET app.tenant='-1'`.
Todas devuelven el MISMO valor en legado y en web (ver "Resultado real").

| ID | Metrica | SQL web (tenant 1) | Legado | Web |
|---|---|---|---|---|
| T01 | Activos | `SELECT count(*) FROM activo WHERE tenant=1` | 68 | 68 |
| T02 | Operaciones | `SELECT count(*) FROM operacion WHERE tenant=1` | 44 | 44 |
| T03 | Cuotas | `SELECT count(*) FROM cronograma_cuota cc JOIN operacion o ON o.operacion=cc.operacion WHERE o.tenant=1` | 459 | 459 |
| T04 | Suma cuotas | `SELECT sum(cc.monto) ...` | 1.224.081.000 | 1.224.081.000 |
| T05 | Cuotas canceladas | `... WHERE cc.estado='CANCELADO'` | 229 | 229 |
| T06 | Saldo por cobrar | `SELECT sum(cc.saldo) ...` | 776.741.670 | 776.741.670 |
| T07 | Recaudado (cobros) | `SELECT sum(monto) FROM cobro WHERE tenant=1` | 447.339.330 | 447.339.330 |
| T08 | Ingresos/egresos | `SELECT count(*) FROM ingreso_egreso WHERE tenant=1` | 56 | 56 |
| T09 | Cobros por mes (dashboard) | `SELECT to_char(fecha,'YYYY-MM'), sum(monto) FROM cobro WHERE tenant=1 GROUP BY 1` | ver T10 | coincide |
| T10 | Cuadre por cuota (estado+fecha) | por (operacion,numero_cuota): estado y fecha_cancelacion == legado | exacto | exacto |

Script de verificacion legado-vs-web: `tools/verifica_0103.py` (imprime la tabla anterior y falla si algo no cuadra).

## Invariantes verificadas

- `sum(cronograma_cuota.saldo)` por documento == `documento.saldo` (coherencia motor).
- Cada cuota CANCELADA tiene `saldo=0` y `fecha_cancelacion` = la del legado (no current_date).
- `sum(cobro.monto)` == suma de cuotas canceladas del legado (447.339.330).
- Distribucion mensual de `cobro.fecha` == distribucion de `FECHA_CANCELACION` del legado (un cobro por cuota).

## Pruebas Manuales

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | Login Pysistemas > Propiedades | 68 activos (38 OCUPADA, 2 VENDIDA) | pendiente (usuario) |
| M02 | Operaciones/contratos + cronograma | 44 contratos; cuotas pagadas canceladas | pendiente (usuario) |
| M03 | Dashboard gerencial con Desde=01/01/2026 | Cobros por mes 2025-06..2026-06; junio=58.595.000 | pendiente (usuario) |
| M04 | Reporte de cobros / cobranza | 229 cobros, total 447.339.330 | pendiente (usuario) |

Nota UX: el dashboard abre en el mes en curso (sin pagos migrados en julio); mover "Desde" al historico.
