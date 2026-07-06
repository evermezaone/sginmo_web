# Codex Review - REQ-0009

Fecha: 2026-07-06
Auditor: codex
Resultado: REQUIERE_CAMBIOS

## Verificacion

- Leidos `req.md`, `claude-implementation.md`, `test-plan.md` y `preaudit-checklist.md`.
- Revisados:
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/EmpresaService.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/EmpresaBean.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/ContextoEmpresa.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/dominio/persona/Persona.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/dominio/persona/PersonaJuridica.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/dominio/persona/PersonaRol.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/dominio/persona/Sucursal.java`
  - `Desarrollo/sginmo-web/src/main/webapp/empresas.xhtml`
  - `Desarrollo/sginmo-web/src/main/webapp/WEB-INF/plantilla.xhtml`
  - `Desarrollo/sginmo-web/src/main/webapp/index.xhtml`
  - `Desarrollo/sginmo-web/src/main/resources/db/migration/V13__empresas_sucursales.sql`
  - `Desarrollo/sginmo-web/src/main/resources/db/migration/V1__esquema_inicial.sql`
- Confirmado correcto:
  - alta de empresa transaccional crea `persona`, `persona_juridica` y rol `EMPRESA`;
  - baja/reactivacion logica de empresa sobre `persona.estado`;
  - sucursales con `por_defecto` unica por empresa dentro de transaccion;
  - contexto `ContextoEmpresa` carga empresa del usuario, sucursales activas y persiste `sucursal_activa` en `preferencia_usuario`;
  - menu/tarjeta de Empresas visibles por permiso;
  - build multi-modulo con JDK 23: `mvn -q clean package` EXIT 0.

## Hallazgos Bloqueantes

### Obs 207 - Edicion de empresa no persiste los datos base de `persona`

Problema: El dialogo de empresas edita campos de `persona` (`numeroDocumento`, `digitoVerificador`, `telefono`, `direccion`, `email`), pero el mapeo `PersonaJuridica.persona` solo tiene `cascade = CascadeType.PERSIST`. En edicion, `EmpresaService.guardar()` ejecuta `em.merge(empresa)` y no hace `merge` explicito de `empresa.getPersona()`. Con una entidad detached de JSF, JPA no propaga cambios al `Persona` asociado si no hay cascade MERGE o merge manual.

Impacto: El usuario puede editar RUC/DV/telefono/direccion/email y recibir mensaje de exito, pero esos datos base no quedan persistidos. Tambien afecta auditoria/version de `persona`, y deja inconsistente la grilla respecto al formulario.

Solucion esperada: Hacer persistente la edicion del agregado completo. Opciones validas: agregar `CascadeType.MERGE` al `@OneToOne`, o en `EmpresaService.guardar()` mergear/actualizar explicitamente `Persona` antes/despues de `PersonaJuridica`. Mantener la validacion de unicidad de documento excluyendo el propio id y cubrir con evidencia de edicion de RUC/telefono/direccion en una empresa existente.

Evidencia:
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/dominio/persona/PersonaJuridica.java:25`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/EmpresaService.java:103`
- `Desarrollo/sginmo-web/src/main/webapp/empresas.xhtml:92`

### Obs 208 - V13 no es reproducible si ya fue aplicada a mano

Problema: El handoff indica `V13__empresas_sucursales.sql` "APLICADA en VPS a mano, Flyway sin cablear". El archivo queda en el repo como migracion Flyway, pero no es tolerante a una base ya parcialmente aplicada: `ALTER TABLE sucursal ADD COLUMN estado` no usa `IF NOT EXISTS` y el `INSERT INTO entidad` de pantalla `empresas` no tiene `ON CONFLICT` ni `WHERE NOT EXISTS`.

Impacto: Cuando Flyway quede cableado o se reconstruya/despliegue sobre una base donde V13 fue aplicada manualmente, el deploy puede fallar por columna duplicada o entidad duplicada. Esto rompe el requisito de deploy verificable y deja la BD dependiendo de pasos manuales.

Solucion esperada: Hacer V13 reproducible o registrar correctamente la aplicacion manual en el historial de Flyway. Para el script: usar `ALTER TABLE ... ADD COLUMN IF NOT EXISTS` y `INSERT ... WHERE NOT EXISTS`/`ON CONFLICT` para entradas seed (`entidad`, `persona_rol`, `sucursal`), respetando constraints reales. Documentar como se valida en base limpia y base ya migrada manualmente.

Evidencia:
- `Desarrollo/sginmo-web/src/main/resources/db/migration/V13__empresas_sucursales.sql:3`
- `Desarrollo/sginmo-web/src/main/resources/db/migration/V13__empresas_sucursales.sql:6`
- `Desarrollo/sginmo-web/src/main/resources/db/migration/V13__empresas_sucursales.sql:11`
- `Desarrollo/sginmo-web/src/main/resources/db/migration/V13__empresas_sucursales.sql:15`

## Pruebas

- Revision estatica de servicios, entidades, XHTML y migracion.
- Build multi-modulo ejecutado con JDK 23:
  - Directorio: `Desarrollo`
  - Comando: `mvn -q clean package`
  - Resultado: EXIT 0

