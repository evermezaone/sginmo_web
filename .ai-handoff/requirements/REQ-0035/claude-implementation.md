# Implementacion Claude - REQ-0035

## Manifiesto Minimo Para Codex
Motor adaptado al V26 en `tools/multiempresa/V27__multiempresa_sps.sql` (staged con V26, se
promueve a db/migration en el deploy). Se recrearon SOLO las 3 funciones que referencian
columnas cambiadas por V26 (las demas quedan intactas):
- f_siguiente_numero: `WHERE tenant=p_empresa AND tipo=p_tipo` (el param conserva el nombre
  historico p_empresa por CREATE OR REPLACE, pero es el tenant).
- f_cobrar_documento: lee `planilla.tenant`/`documento.tenant`; valida coherencia de tenant
  (planilla=documento); `documento.tipo` directo; inserta `cobro.tenant`; en dato_cobro
  resuelve emisor/procesador/motivo_rechazo por id via subconsulta a entidad con
  `tenant IN (-1, v_tenant)`.
- f_anular_cobro: lee `cobro.tenant`; resuelve el motivo por id (MOTIVOS_ANULACION,
  `tenant IN(-1,v_tenant)`) y RAISE si no existe; inserta `anulacion.tenant, motivo`.

SQL nativo Java: OperacionService (INSERT/SELECT documento con tenant+empresa, tipo),
ReporteService (empresa->tenant en cobro/operacion/planilla/activo, tipo directo), InicioBean
(dashboard empresa->tenant), CajaService (tipo directo).

**Archivos:** tools/multiempresa/V27__multiempresa_sps.sql (+ v27_test.sql); servicio/{OperacionService,
ReporteService, CajaService}.java; web/InicioBean.java.

**Comandos probados:** `BEGIN; <V26>; <V27>; <prueba funcional>; ROLLBACK;` via psql ON_ERROR_STOP
contra la BD real -> EXIT 0 (T1 numero=1; T2 cobro tenant=1 saldo=30000 caja=70000; T3 motivo
inexistente rechazado; T4 anulacion tenant=1 motivo_id resuelto, saldo repuesto=100000 caja=0).
`mvn -q -pl sginmo-web -am -DskipTests package` -> EXIT 0.
