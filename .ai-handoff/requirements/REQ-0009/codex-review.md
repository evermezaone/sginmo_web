# Codex Review - REQ-0009

Fecha: 2026-07-07
Auditor: codex
Resultado: APROBADO_POR_CODEX en ronda 2

## Verificacion

- Leidos `req.md`, `claude-implementation.md`, `test-plan.md`, `preaudit-checklist.md` y observaciones registradas en `AUDITORIA_OBSERVACION`.
- Reauditadas observaciones previas:
  - Obs 207: corregida. `PersonaJuridica.persona` ahora usa `cascade = {CascadeType.PERSIST, CascadeType.MERGE}`; `PersonaFisica` fue alineada por consistencia. La edicion de empresa via `em.merge(empresa)` ya propaga cambios a `persona`.
  - Obs 208: corregida. `V13__empresas_sucursales.sql` usa `ADD COLUMN IF NOT EXISTS` y `INSERT ... WHERE NOT EXISTS` para pantalla, rol y sucursal semilla. El patron de `ON CONFLICT`/guardas tambien se ve aplicado en migraciones equivalentes muestreadas (`V10`, `V12`, `V14`).
- Revisados:
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/dominio/persona/PersonaJuridica.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/dominio/persona/PersonaFisica.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/EmpresaService.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/ContextoEmpresa.java`
  - `Desarrollo/sginmo-web/src/main/resources/db/migration/V13__empresas_sucursales.sql`
  - `Desarrollo/sginmo-web/src/main/resources/db/migration/V10__grupos_seguridad.sql`
  - `Desarrollo/sginmo-web/src/main/resources/db/migration/V12__abms_catalogo.sql`
  - `Desarrollo/sginmo-web/src/main/resources/db/migration/V14__pantalla_personas.sql`

## Resultado

Sin observaciones bloqueantes.

Riesgo menor no bloqueante: `EmpresaService.subRol()` no filtra `PersonaRol.estado='ACTIVO'`, aunque el texto funcional define empresa como persona juridica con rol EMPRESA activo. En este REQ la baja logica de empresa esta definida sobre `persona.estado` y no hay flujo de inactivacion del rol EMPRESA; queda como punto a observar cuando se desarrollen roles/personas completos.

Nota: `claude-implementation.md` conserva frases viejas sobre `cascade PERSIST` y "Flyway sin cablear"; la evidencia vigente esta en `preaudit-checklist.md`, BD y codigo real. No bloquea el cierre.

## Pruebas

- Revision estatica de servicios, entidades y migraciones.
- Build multi-modulo ejecutado con JDK 23:
  - Directorio: `Desarrollo`
  - Comando: `mvn -q clean package`
  - Resultado: EXIT 0

