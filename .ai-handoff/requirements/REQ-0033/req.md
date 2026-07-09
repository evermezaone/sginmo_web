# REQ-0033 - Multiempresa F1: migracion de esquema (V26)

**Estado:** implementado y verificado por rollback (2026-07-09)

## Objetivo Funcional
Transformar el esquema V25 al modelo multiempresa real (doc 14 v2.2), sin mezclar
datos entre empresas. Discriminador `tenant bigint NOT NULL` (FK persona_juridica),
invisible en UI; `tenant = -1` = registro GLOBAL bloqueado (solo SUPERADMIN).

Cambios de la migracion `V26__multiempresa_esquema.sql`:
- **entidad** rediseñada: PK numerica autonumerica + `UNIQUE(lista, codigo, tenant)`;
  la columna historica `entidad` (nombre de lista) pasa a `lista`. Las 29 FK compuestas
  `*_lista/*_codigo` de las tablas referenciantes se reemplazan por UNA columna `bigint`
  FK a `entidad(entidad)` (mapeo por `(lista,codigo)->id`).
- **tipo de comprobante** DIRECTO `varchar(25)` en `documento` y `rango_comprobante`
  (REC/FACT/FACP/NTCR/DINT...); la lista TIPOS_DOCUMENTO deja de ser catalogo.
- **RENAME empresa->tenant** (8 tablas): usuario, operacion, activo (+NOT NULL),
  planilla, cobro, ingreso_egreso, anulacion, rango_comprobante.
- **documento**: ADD `tenant` (dueño del registro) + CONSERVA `empresa` (emisora
  comercial/fiscal); `UNIQUE(tenant, empresa, tipo, serie, numero)`.
- **catalogos** ganan `tenant` (moneda, impuesto, forma_pago, articulo, atributo,
  atributo_por_tipo, ubicacion_geografica); semillas de sistema a `-1`; unicidades
  por-codigo reemplazadas por `(tenant, codigo)` (forma_pago, articulo, grupo).
- **persona** reducida a identidad; datos comerciales/contextuales movidos a la NUEVA
  `persona_empresa` (por tenant). `persona_rol` +tenant (rol EMPRESA identitario en -1).
- **parametro_sistema** PK `(tenant, clave)`; `sucursal`/`grupo` +tenant.
- **usuario.perfil** gana `SUPERADMIN` + primer superadmin en tenant -1.
- Vistas `v_persona` / `v_operacion_saldo` recreadas exponiendo `tenant`.

## Criterios De Aceptacion
- [x] V26 deriva de la estructura real V25 (pg_dump) con nombres exactos.
- [x] Toda la migracion corre sin error contra los datos reales (BEGIN...ROLLBACK, EXIT=0).
- [x] 0 pares `*_lista/*_codigo` restantes; 0 columnas `empresa` fuera de `documento`.
- [x] 8276 ubicaciones mapean su `nivel` sin nulos; 27 FK de 1 columna a `entidad`, 0 compuestas.
- [x] `persona_empresa` poblada; columnas comerciales removidas de persona/fisica/juridica.
- [x] Fila GLOBAL -1, superadmin, PK `(tenant,clave)` y perfil SUPERADMIN presentes.

## Bloqueo Formal Documentado
La migracion NO se aplica aun a la BD viva: rompe la app desplegada (las entidades JPA
y los SP todavia referencian empresa/tipo_codigo/*_lista). Se aplicara junto con F2
(REQ-0034, entidades) y F3 (REQ-0035, SP) como unidad desplegable. La BD viva sigue en V25.

## Compuerta de despliegue (obs 243)
Para que el bloqueo sea EFECTIVO y no solo documental, V26 NO se entrega en el path
activo de Flyway. `FlywayMigrator` corre `flyway.migrate()` sobre `classpath:db/migration`
en cada arranque; si V26 estuviera ahi, el proximo deploy la aplicaria sola. Por eso V26
vive en `tools/multiempresa/V26__multiempresa_esquema.sql` (staging, fuera del classpath
del WAR) con su evidencia (gen + bateria + README). Se PROMUEVE a
`Desarrollo/sginmo-web/src/main/resources/db/migration/` recien cuando F2+F3 formen la
unidad desplegable. Asi ningun deploy puede aplicar V26 antes de tiempo.
