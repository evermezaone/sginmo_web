# REQ-0103 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-16
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0103
- Tipo de cambio: BD (carga de datos) + herramientas de migracion (no toca la app)
- Riesgo: medio (escribe datos reales en PostgreSQL prod, tenant 1)
- Archivos clave:
  - `tools/migra_0103_personas.py`: genera SQL idempotente de personas+roles desde SOCIOS_NEGOCIOS.
  - `tools/migra_0103_relacional.py`: migracion relacional (limpieza, activos, operaciones, cronogramas via motor, propietarios, marcado de cuotas pagadas, ingresos/egresos).
- Comandos probados (todos ejecutados contra la VPS via tunel 127.0.0.1:15432):
  - `python tools/migra_0103_personas.py` + pipe a psql: 74 personas / 72 roles.
  - `python tools/migra_0103_relacional.py --clean-activos`: borra data de prueba del tenant 1 (DELETEs por orden FK con savepoints).
  - `python tools/migra_0103_relacional.py --activos`: 7 entidades + 61 propiedades.
  - `python tools/migra_0103_relacional.py --operaciones`: 44 operaciones + 459 cuotas (f_generar_cronograma).
  - `python tools/migra_0103_relacional.py --propietarios --marcar-cuotas`: 7 propietarios, 229 cuotas pagadas.
  - `python tools/migra_0103_relacional.py --ingresos`: 23 articulos + 56 egresos.
- Cambios de datos: SI. Carga inicial de datos reales del legado en tenant 1 (Pysistemas). Se borro previamente la data de prueba del tenant 1 (autorizado por el usuario: "los datos que estan en la web son todos pruebas"). Backup PostgreSQL tomado antes de aplicar.
- Cambios de entorno: no.
- Impacto LLM/tokens: no.
- Decision esperada: aprobar (o revisar riesgo de carga de datos).
- Notas para auditor: la fuente legada NO tiene tabla DOCUMENTOS (la cuenta corriente son las cuotas del cronograma + ingresos/egresos). Por eso los cobros NO se migraron como filas de la tabla `cobro` (que exige `planilla`/caja); en su lugar se replico el estado de pago copiando `CANCELADO`+`fecha_cancelacion` del legado a `cronograma_cuota`, dejando los informes de cobranza/mora/pendientes comparables. Ver "Riesgos Conocidos".

## Resumen Funcional

Se cargaron los datos reales del sistema WinForms legado (Firebird `INMOBILIARIA.FDB`) en la empresa
Pysistemas (tenant 1) del sistema web, para que el usuario pueda ver y comparar los informes contra el
sistema viejo: propiedades, contratos/operaciones, cronogramas de cuotas (pagadas y pendientes),
propietarios y la caja de egresos.

## Resumen Tecnico

Migracion por fases leyendo Firebird 2.5 embebido (`fbembed.dll` + driver `fdb`) y escribiendo en
PostgreSQL destino (VPS, via tunel SSH `127.0.0.1:15432`, `psycopg2`), con `SET app.tenant='-1'`
(superadmin) para cruzar RLS. Se construyen mapas de ID legado->nuevo en memoria/JSON y se usan los SP
del motor (`f_generar_cronograma`) para que las cuotas las calcule el sistema, no cargadas a mano.

Mapeo legado -> web:
- SOCIOS_NEGOCIOS -> persona (+persona_fisica/juridica) + persona_rol (idempotente por numero_documento).
- ENTIDADES_INMOBILIARIAS -> activo (padre); PROPIEDADES -> activo (hijo, padre=entidad).
- PROPIETARIOS_ENT_INMOB -> activo_propietario (enlace por ENTIDAD; PROPIEDAD_ID viene null en el legado).
- OPERACIONES_PROPIEDADES -> operacion; cronograma via f_generar_cronograma.
- CRONOGRAMAS_CUOTAS.ESTADO/FECHA_CANCELACION -> se replica marcando `cronograma_cuota` como CANCELADO.
- ITEMS_INGRESOS_EGRESOS -> articulo (SERVICIO, impuesto Exenta, codigo MIG-ITEM-N, idempotente).
- INGRESOS_EGRESOS -> ingreso_egreso (tenant 1).

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| `tools/migra_0103_personas.py` | Nuevo: SQL idempotente personas+roles. |
| `tools/migra_0103_relacional.py` | Nuevo: migracion relacional completa (7 fases via flags). |

## Cambios De Datos

Carga inicial en tenant 1. Verificacion legado vs web:

| Entidad | Legado | Web (tenant 1) |
|---|---|---|
| Activos (entidades+propiedades) | 68 | 68 |
| Operaciones | 44 | 44 |
| Cuotas cronograma | 459 | 459 |
| Cuotas pagadas (CANCELADO) | 229 | 229 |
| Ingresos/egresos | 56 | 56 |
| Propietarios enlazados | 7 | 7 |
| Personas con documento | 69 | 69 (idempotente) |

Backup previo tomado (pg_dump con `--enable-row-security` + `PGOPTIONS='-c app.tenant=-1'`).

## Variables De Entorno

Sin cambios (usa APP_DB_PASS del `.env` local para el tunel).

## Pruebas Ejecutadas

- Todas las fases ejecutadas sin error contra la VPS; conteos verificados con consultas de control
  (comparacion legado vs web arriba). Idempotencia probada en personas (re-run no duplica por documento)
  y en articulos (codigo MIG-ITEM-N).

## Pruebas Manuales Sugeridas

1. Entrar al web como usuario de Pysistemas y abrir el listado de Propiedades: deben verse las 68.
2. Abrir Operaciones/contratos: 44, con su cronograma; verificar que las cuotas pagadas figuran canceladas.
3. Reporte de cobranza/mora: pendientes 230, pagadas 229.
4. Caja de egresos: 56 movimientos.

## Correcciones post-auditoria (obs 320/321/322)

- **obs 321 (montos exactos)** — `tools/migra_0103_exacto.py`: se reconstruyo la capa cronograma+documento+
  cobros del tenant 1 usando los importes/fechas/estados VERBATIM del legado (CRONOGRAMAS_CUOTAS), sin
  regenerar con f_generar_cronograma. Verificacion legado==web EXACTA: cuotas 459, suma Gs 1.224.081.000,
  canceladas 229, saldo por cobrar 776.741.670, recaudado 447.339.330.
- **Capa financiera coherente** — `tools/migra_0103_financiero.py` (y luego exacto): cada operacion tiene su
  documento interno DINT/OP (como OperacionService.crearDocumentoInterno), las cuotas enlazadas, y los cobros
  hechos por `f_cobrar_documento` en una planilla de migracion -> el saldo baja de verdad y la recaudacion es
  visible en el dashboard. Se corrigio ademas activo.estado (ALQUILER vigente=OCUPADA 38, VENTA=VENDIDA 2).
- **obs 320 (idempotencia) / obs 322 (autorizacion)** — ver `user-decision.md`: es una carga inicial de una
  sola vez, destructiva por diseno, con autorizacion explicita del usuario y backup previo (rollback=restore).

## Correcciones ronda 2 (obs 323/324/325)

- **obs 323 (estado/fecha por cuota historicos)**: (a) un cobro POR CUOTA pagada con su `FECHA_CANCELACION`
  real (o vencimiento si el legado no la registro); (b) paso 5b en `migra_0103_exacto.py` que sobrescribe
  `cronograma_cuota.estado/saldo/fecha_cancelacion` con los valores EXACTOS del legado por
  (operacion, numero_cuota), despues de los cobros (defeat del FIFO + current_date que aplica el motor).
- **obs 324/325 (evidencia reproducible)**: `tools/verifica_0103.py` compara legado vs web y falla si algo
  no cuadra; salida guardada en `verificacion-cuadre.txt`. test-plan.md y preaudit-checklist.md actualizados.

## Evidencia de cuadre (tools/verifica_0103.py -> verificacion-cuadre.txt)

RESULTADO: TODO CUADRA. activos 68=68, operaciones 44=44, cuotas 459=459, suma 1.224.081.000=igual,
canceladas 229=229, saldo 776.741.670=igual, recaudado 447.339.330=igual, ingresos/egresos 56=56.
Cobros por mes legado==web en los 13 meses (2025-06..2026-06). Cuadre por cuota: 459 comparadas, 0 mismatches.

## Riesgos Conocidos

- Ninguno pendiente. El estado, saldo y fecha de cancelacion de cada cuota coinciden exactamente con el
  legado (verificado por cuota, 0 discrepancias); los cobros conservan su fecha real de pago.
