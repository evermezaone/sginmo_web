# Plan de Pruebas - REQ-0033 (V26 multiempresa)

## Metodo
PostgreSQL aplica DDL transaccional. Se corrio TODA la migracion V26 dentro de una
transaccion contra los DATOS REALES de la VPS (77.237.235.69, BD sginmo), seguida de
una bateria de asserts, y luego `ROLLBACK` — verifica que aplica sin un solo error y
sin persistir nada (la BD viva sigue intacta en V25).

Comando: `psql -v ON_ERROR_STOP=1 -f v26_bateria.sql` (BEGIN + V26 + tools/multiempresa/v26_checks.sql + ROLLBACK).
Resultado global: **EXIT=0** (ningun statement fallo; el ON_ERROR_STOP habria abortado ante cualquier error).

## Evidencia (asserts, todos en verde)
| Check | Esperado | Obtenido |
|---|---|---|
| entidad total / globales | 108 / 108 | 108 / 108 |
| entidad PK tipo | bigint | bigint |
| pares `*_lista/*_codigo` restantes | 0 | 0 |
| columnas `empresa` restantes | solo `documento` | documento |
| ubicacion_geografica `nivel` nulos | 0 | 0 (8276 mapeadas) |
| persona_rol | EMPRESA en -1, PROVEEDOR en 1 | `1 t=-1 rol=36` / `1 t=1 rol=28` |
| persona_empresa filas / tenants | 2 / {1} | 2 / tenants=1 |
| persona cols movidas restantes | 0 | 0 |
| documento columnas clave | empresa,tenant,tipo | empresa,tenant,tipo |
| parametro_sistema PK | (tenant, clave) | PRIMARY KEY (tenant, clave) |
| usuario perfil check | incluye SUPERADMIN | SUPERADMIN,ADMINISTRADOR,USUARIO |
| superadmin creado | tenant -1 | superadmin perfil=SUPERADMIN t=-1 |
| persona GLOBAL -1 | existe | -1 GLOBAL |
| v_persona consultable | si | 3 filas |
| v_operacion_saldo consultable | si | 0 filas |
| catalogos con tenant<>-1 | 0 | 0 |
| FKs compuestas a entidad | 0 | 0 |
| FKs de 1 col a entidad | ~27 | 27 |
| articulo UNIQUE (tenant,codigo) | existe | 1 |

## Pendiente (fuera de F1)
- Aplicacion real a la BD viva: junto con F2/F3 (unidad desplegable). Hasta entonces V25.
- Verificacion funcional con 2 empresas: REQ-0039 (F7).
