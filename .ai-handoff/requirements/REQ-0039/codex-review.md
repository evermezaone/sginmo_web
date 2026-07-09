# Codex Review - REQ-0039

Estado: APROBADO_POR_CODEX
Fecha: 2026-07-09T20:15:15-03:00
Auditor: codex

## Resultado

REQ-0039 queda aprobado. La verificacion integral F7 fue reproducida contra la BD real de la VPS, como rol de aplicacion `sginmo`, envolviendo V26+V27+V28+`f7_integracion_test.sql` en una unica transaccion `BEGIN ... ROLLBACK`.

## Evidencia verificada

- Bateria ejecutada: `tools/multiempresa/V26__multiempresa_esquema.sql` + `V27__multiempresa_sps.sql` + `V28__multiempresa_rls.sql` + `f7_integracion_test.sql`.
- Comando remoto: `psql -h localhost -U sginmo -d sginmo -v ON_ERROR_STOP=1 -f -`.
- Resultado: `F7 INTEGRACION OK - 2 empresas aisladas, superadmin total, cross-tenant negado`; `ROLLBACK` ejecutado; EXIT 0.
- Rol app confirmado: `rolsuper=f`, `rolbypassrls=f`.
- BD viva confirmada intacta: `flyway_schema_history max(version) = 25`.
- Build de la app con el codigo vigente: `mvn -q -pl sginmo-web -am clean package` en `Desarrollo` -> EXIT 0.

## Cobertura auditada

- SUPERADMIN con `app.tenant=-1` ve ambas sucursales.
- Tenant A ve sucursal propia y catalogo global+propio, no datos de B.
- Tenant B queda aislado de A.
- INSERT cross-tenant queda negado por RLS `WITH CHECK`.
- UPDATE cross-tenant afecta 0 filas.
- "Operar como" se modela con `app.tenant=A` y acota igual que un administrador de A.
- Seguridad fuera de RLS (`usuario`, `grupo`, `usuario_grupo`, permisos) queda cubierta por las guardas cerradas en REQ-0038.

## Nota de alcance

El login HTTP real con usuarios de dos empresas queda correctamente diferido al pos-deploy de la unidad V26+V27+V28, porque aplicar V26 sola romperia la app viva en V25. Para este REQ, la defensa RLS y el aislamiento de capa service quedan verificados con evidencia reproducible.
