# Codex Review - REQ-0037

Estado: REQUIERE_CAMBIOS
Fecha: 2026-07-09T19:14:53-03:00
Auditor: codex

## Observaciones bloqueantes

### Obs 255 - InicioBean consulta tablas con RLS sin fijar app.tenant

`TenantInterceptor` solo se aplica a clases/metodos con `@AislarTenant`. `InicioBean` inyecta `EntityManager` directamente y ejecuta SQL nativo sobre tablas cubiertas por V28 (`activo`, `operacion`, `cobro`) sin pasar por un service anotado ni ejecutar `set_config('app.tenant', ..., true)`:

- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/InicioBean.java:22`: `@PersistenceContext` directo en el bean web.
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/InicioBean.java:57-65`: consultas sobre `activo`, `operacion`, `cobro` y `v_operacion_saldo`.
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/InicioBean.java:70` y `:79`: ejecución directa con `em.createNativeQuery(...)`.

Impacto: con V28 activa, esas consultas corren sin `app.tenant`; las tablas transaccionales quedan invisibles por RLS y el tablero mostrara ceros aunque existan datos del tenant. Peor: las excepciones se silencian en `num()`/`dec()`, por lo que el fallo queda oculto.

Solucion esperada: mover esos KPIs a un `InicioService`/`DashboardService` anotado con `@AislarTenant` y `@Transactional`, o fijar `app.tenant` mediante el mismo mecanismo centralizado antes de ejecutar consultas. El bean JSF no debe acceder directamente a tablas bajo RLS.

### Obs 256 - v28_test.sql puede dar EXIT 0 aunque falle el aislamiento

El plan de pruebas declara que la bateria RLS valida aislamiento real, pero `tools/multiempresa/v28_test.sql` no tiene asserts: solo imprime `NOTICE`.

- `tools/multiempresa/v28_test.sql:18-23`: si el insert cross-tenant no es bloqueado, el script ejecuta `RAISE NOTICE 'T-write FALLO...'`, pero no lanza error.
- `tools/multiempresa/v28_test.sql:29-50`: los conteos esperados se imprimen con `RAISE NOTICE`; no hay `IF v_cnt <> esperado THEN RAISE EXCEPTION`.

Impacto: `psql ON_ERROR_STOP` puede terminar `EXIT 0` aun cuando RLS permita insertar otro tenant o los conteos sean incorrectos. Eso invalida la evidencia principal del criterio "aislamiento real probado".

Solucion esperada: convertir la bateria en asserts duros (`RAISE EXCEPTION` si el resultado difiere). El caso cross-tenant debe fallar si el `INSERT` no lanza excepcion. Los conteos de tenant 1, tenant 500, SUPERADMIN, catalogos y sin `app.tenant` deben abortar cuando no coinciden con lo esperado.

## Verificacion realizada

- Leidos `req.md`, `claude-implementation.md`, `preaudit-checklist.md` y `test-plan.md`.
- Inspeccionados `tools/multiempresa/V28__multiempresa_rls.sql`, `tools/multiempresa/v28_test.sql`, `TenantInterceptor`, `AislarTenant`, `beans.xml`, `TenantContext` y accesos directos a `EntityManager`.
- Verificada cobertura de services con `@AislarTenant`/`@Transactional`.

No se ejecuta build como criterio de aprobacion porque el REQ queda rechazado por brecha runtime en `InicioBean` y por evidencia de prueba no asertiva.

---

## Reauditoria - 2026-07-09T19:29:00-03:00

Estado: APROBADO_POR_CODEX

### Obs 255 - Cerrada

Verificado:

- `InicioBean` ya no inyecta `EntityManager` ni ejecuta SQL directo contra tablas bajo RLS.
- Los KPIs fueron movidos a `InicioService`, anotado con `@AislarTenant` y `@jakarta.transaction.Transactional`.
- `InicioService.kpis()` corre dentro del mecanismo centralizado que fija `app.tenant` y ya no silencia excepciones de consulta.

### Obs 256 - Cerrada

Verificado:

- `tools/multiempresa/v28_test.sql` ahora usa asserts duros con `RAISE EXCEPTION`.
- El intento de `INSERT` cross-tenant aborta el test si RLS no lanza `insufficient_privilege` o `check_violation`.
- Los conteos de tenant 500, tenant 1, SUPERADMIN, catalogos globales y fail-closed sin `app.tenant` abortan si no cumplen lo esperado.
- Los conteos usan marcador `ACT-RLS%`, evitando depender de datos preexistentes.

### Resultado final

Obs 255 y 256 cerradas. REQ-0037 cumple la defensa RLS en BD y el seteo de `app.tenant` en la capa Java revisada.

Verificacion ejecutada:

- `mvn -q -pl sginmo-web -am clean package` - EXIT 0
