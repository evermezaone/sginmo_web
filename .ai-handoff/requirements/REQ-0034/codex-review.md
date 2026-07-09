# Codex Review - REQ-0034

Estado: REQUIERE_CAMBIOS
Fecha: 2026-07-09T10:56:00-04:00
Auditor: codex

## Observaciones bloqueantes

### Obs 245 - Entidades/servicios persistidos no reflejan columnas tenant y PK compuesta de V26

REQ-0034 acepta como alcance que toda la capa JPA/DTO/UI refleje el esquema V26. Sin embargo, quedan entidades persistidas que siguen mapeadas como V25 aunque V26 ya cambia su estructura:

- `parametro_sistema`: V26 agrega `tenant bigint NOT NULL`, FK a `persona_juridica`, y cambia la PK a `(tenant, clave)` (`tools/multiempresa/V26__multiempresa_esquema.sql:351-357`). La entidad `ParametroSistema` sigue con `@Id clave` solamente y no tiene campo `tenant` (`ParametroSistema.java:15-17`). `ParametroService.guardar` todavia hace `em.find(ParametroSistema.class, clave)` (`ParametroService.java:55`), incompatible con una PK compuesta.
- `sucursal`: V26 agrega `tenant bigint NOT NULL` (`tools/multiempresa/V26__multiempresa_esquema.sql:359-364`). La entidad `Sucursal` no mapea `tenant` (`Sucursal.java:17-39`) y `EmpresaService.guardarSucursal` persiste `Sucursal` sin asignarlo (`EmpresaService.java:169-201`). Tras aplicar V26, el alta de sucursal fallaria por `tenant` nulo o quedaria sin forma de expresar el tenant real en JPA.
- `grupo`: V26 agrega `tenant bigint NOT NULL` y reemplaza la unicidad global por `UNIQUE(tenant, codigo)` (`tools/multiempresa/V26__multiempresa_esquema.sql:365-370`). La entidad `Grupo` no mapea `tenant` y mantiene `@Column(unique = true)` sobre `codigo` (`Grupo.java:17-28`), mientras `GrupoService` valida duplicidad global por codigo (`GrupoService.java:67-73`).

Impacto: F2 compila, pero no queda coherente con el esquema V26. Al desplegar V26+F2+F3, ABMs de parametros, sucursales y grupos pueden fallar en runtime o conservar reglas globales que el esquema ya hizo por tenant. Esto contradice el criterio de aceptacion de "Todas las entidades JPA reflejan el esquema V26".

Solucion esperada: adaptar esos mapeos y sus servicios al esquema V26 dentro de F2. Como minimo: `ParametroSistema` con id compuesto `(tenant, clave)` y queries/`find` acordes; `Sucursal.tenant` seteado desde `personaJuridica` o contexto segun regla V26; `Grupo.tenant` y unicidad/validacion por `(tenant, codigo)` con default/global `-1` mientras el aislamiento fino quede para F4/F6.

## Verificacion realizada

- Leido `req.md`, `claude-implementation.md`, `preaudit-checklist.md`, `test-plan.md` y `analysis.md`.
- Inspeccionado `tools/multiempresa/V26__multiempresa_esquema.sql`.
- Inspeccionados `ParametroSistema.java`, `ParametroService.java`, `Sucursal.java`, `EmpresaService.java`, `Grupo.java` y `GrupoService.java`.

No se ejecuta build como criterio de aprobacion porque el REQ queda rechazado por incoherencia estructural JPA vs V26.

---

## Reauditoria - 2026-07-09T13:00:00-04:00

Estado: APROBADO_POR_CODEX

### Obs 245 - Cerrada

Verificado en codigo real:

- `ParametroSistema` ahora usa `@IdClass(ParametroSistemaId.class)` con `tenant` + `clave`; `ParametroService.guardar` arma `ParametroSistemaId(tenant, clave)` para el `find`.
- `Sucursal` ahora mapea `tenant`; `EmpresaService.guardarSucursal` asigna `sucursal.setTenant(sucursal.getPersonaJuridica())` antes de persistir/mergear.
- `Grupo` ahora mapea `tenant`; `GrupoService.guardar` asigna tenant por contexto y valida duplicados por `(tenant, codigo)`.

Verificacion final:

- Revisados `ParametroSistema.java`, `ParametroSistemaId.java`, `ParametroService.java`, `Sucursal.java`, `EmpresaService.java`, `Grupo.java`, `GrupoService.java`.
- Build reactor desde `Desarrollo`: `mvn -q clean package` -> EXIT 0.

Sin riesgos bloqueantes restantes para el alcance F2.
