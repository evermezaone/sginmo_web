# REQ-0035 - Multiempresa F3: SPs del motor + SQL nativo adaptados al V26

**Estado:** implementado; verificado por rollback + `mvn package` verde (2026-07-09)

## Objetivo Funcional
Adaptar el motor de dinero (funciones PL/pgSQL) y el SQL nativo de la capa Java al esquema
V26, con validacion de coherencia de tenant. Solo 3 funciones referencian columnas cambiadas:
- **f_siguiente_numero**: numera por `rango_comprobante.tenant` + `tipo` directo.
- **f_cobrar_documento**: planilla/cobro/documento por `tenant`; **coherencia de tenant**
  (la planilla y el documento deben ser del mismo tenant); `documento.tipo` directo;
  `dato_cobro.emisor/procesador/motivo_rechazo` por **id** (resueltos desde el codigo por
  `(lista, codigo, tenant IN(-1, tenant_del_documento))`).
- **f_anular_cobro**: `cobro.tenant`; `anulacion.tenant` + `motivo` por **id** (validado
  contra MOTIVOS_ANULACION visible al tenant).
Las demas (f_cuadrar_documento, f_actualiza_saldo_cuotas, f_mora_cuota, f_generar_cronograma,
f_cobrar_total, triggers) NO tocan columnas cambiadas.

SQL nativo Java adaptado: `OperacionService` (documento tenant+empresa, tipo directo),
`ReporteService` (cobro/operacion/planilla/activo empresa->tenant, tipo directo), `InicioBean`
(dashboard: activo/operacion/cobro empresa->tenant), `CajaService` (tipo directo).

## Criterios De Aceptacion
- [x] V27 (las 3 funciones) crea sin error sobre el esquema V26.
- [x] Prueba funcional del motor sobre V26+V27: numerar -> cobrar -> anular con motivo por id,
      con coherencia de tenant, saldos/caja correctos y motivo inexistente rechazado.
- [x] SQL nativo Java sin referencias a columnas renombradas (grep) y WAR empaqueta verde.

## Bloqueo Formal Documentado
V26 + V27 + F2 (REQ-0034) se despliegan como UNIDAD; no se aplican a la BD viva todavia.
V27 vive en `tools/multiempresa/` (staging) y se promueve a `db/migration/` junto con V26 en
el deploy. El aislamiento por tenant en los services (filtros de lectura, pertenencia) es F4.
