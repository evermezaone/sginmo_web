# REQ-0103 - Migracion de datos del legado Firebird al sistema web

**Numero:** REQ-0103
**Fecha de creacion:** 2026-07-16
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

"Crea un requerimiento para migrar datos existentes en
migracion/source (INMOBILIARIA.FDB) al sistema web."

## Contexto

- Origen: `source/INMOBILIARIA.FDB` (Firebird 2.5, ~15 MB) — base del sistema WinForms legado.
- Destino: PostgreSQL del sistema web (sginmo, en la VPS), multiempresa con RLS por tenant.
- Ya existe un ETL de go-live: `tools/etl_firebird_postgres.py` (REQ-0031), hoy en dry-run con
  mapeos declarativos placeholder (se completaron parcialmente porque el legado tenia pocos/ningun dato).
  Ahora el usuario proveyo el .fdb real, asi que corresponde completar y EJECUTAR la migracion.

## Objetivo Funcional

Cargar los datos reales del legado en el sistema web de forma consistente (saldos, cronogramas y cuentas
corrientes cuadrados), idempotente y verificable, sin romper el aislamiento por empresa (RLS).

## Alcance / Plan por fases

1. **Habilitar lectura del .fdb**: instalar/portar `fbclient` (Firebird client) para que el driver `fdb`
   pueda abrir INMOBILIARIA.FDB (o usar Firebird embebido portable). Sin esto no se lee el origen.
2. **Descubrimiento del esquema real**: leer `RDB$RELATIONS`/`RDB$RELATION_FIELDS` + conteos por tabla;
   mapear tablas y columnas legadas reales a las nuevas (documentar el mapeo definitivo).
3. **Completar los mapeos del ETL** contra el esquema real (personas/roles, activos/inmuebles,
   operaciones/contratos, cronograma/cuotas, cobros/recibos, ingresos/egresos).
4. **Dry-run** (`--dry-run`): reportar que insertaria, detectar faltantes/inconsistencias, revisar.
5. **Carga (`--apply`)** idempotente contra el PostgreSQL destino, respetando el orden de dependencias y
   usando los SP/triggers del motor (`f_cobrar_documento`, `f_generar_cronograma`) para que los saldos y
   cronogramas los calcule el sistema (no cargar saldos "a mano" que rompan la trazabilidad).
6. **Verificacion**: conteos origen vs destino, integridad de saldos/cronogramas, RLS por tenant, muestreo
   manual de casos (un cliente con contrato + cuotas + cobros).

## Decisiones necesarias del usuario (antes de --apply)

- **Empresa/tenant destino**: a que empresa del sistema web se cargan estos datos (el legado es mono-empresa;
  el web es multiempresa). Confirmar el tenant destino.
- **Carga inicial vs incremental**: asumo carga INICIAL sobre una empresa vacia (o recien creada). Si la
  empresa ya tiene datos, definir estrategia (idempotencia por clave natural evita duplicar).
- **Backup previo**: tomar backup del PostgreSQL destino antes del --apply (REQ-0065 lo permite).
- **Alcance de tablas**: migrar todo o un subconjunto (ej. solo clientes+contratos+cuotas, sin historicos
  de gastos), segun lo que exista y sea util.

## Criterios De Aceptacion

- [ ] El ETL lee INMOBILIARIA.FDB y reporta el esquema y conteos reales.
- [ ] Los mapeos del ETL cubren las tablas con datos utiles del legado.
- [ ] Dry-run sin errores y revisado.
- [ ] Apply idempotente: re-correr no duplica (clave natural).
- [ ] Post-carga: conteos y saldos/cronogramas consistentes; datos visibles en el web bajo el tenant destino con RLS.
- [ ] No se rompe el aislamiento por empresa ni la trazabilidad de pagos.

## Dependencias

- ETL existente: `tools/etl_firebird_postgres.py` (REQ-0031). Motor: `f_cobrar_documento`, `f_generar_cronograma`.
- Requiere `fbclient` (Firebird client) en la estacion. Backup destino (REQ-0065).
- Decision de negocio: tenant/empresa destino + alcance.
