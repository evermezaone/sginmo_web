# Codex Review - REQ-0011

Fecha: 2026-07-07
Auditor: codex
Resultado: REQUIERE_CAMBIOS

## Verificacion

- Leidos `req.md`, `claude-implementation.md`, `test-plan.md` y `preaudit-checklist.md`.
- Revisados:
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/dominio/catalogo/ParametroSistema.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/ParametroService.java`
  - `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/ParametroBean.java`
  - `Desarrollo/sginmo-web/src/main/webapp/parametros.xhtml`
  - `Desarrollo/sginmo-web/src/main/resources/db/migration/V1__esquema_inicial.sql`
  - `Desarrollo/sginmo-web/src/main/resources/db/migration/V2__seed_basico.sql`
  - `Desarrollo/sginmo-web/src/main/resources/db/migration/V4__version_optimista.sql`
  - `Desarrollo/sginmo-web/src/main/resources/db/migration/V5__seed_seguridad.sql`
  - `Desarrollo/sginmo-web/src/main/resources/db/migration/V11__parametros_smtp.sql`
  - `Desarrollo/onesystem-security/src/main/java/py/com/one/core/Auditable.java`

Confirmado correcto:

- La grilla usa `LazyDataModel` y busca por clave/descripcion en BD.
- `ParametroSistema.isSensible()` enmascara claves que contienen `CLAVE` o `PASS` en la grilla.
- La clave queda deshabilitada al editar y el bean fuerza mayusculas en alta.
- No hay baja en UI ni servicio para `parametro_sistema`.
- `ParametroService.guardar()` exige permisos backend `CREAR`/`EDITAR`.
- `Auditable` mapea `@Version` y `ParametroService` traduce `OptimisticLockException`.
- `V11__parametros_smtp.sql` no contiene credenciales reales y deja `SMTP_CLAVE` vacia.

## Hallazgos Bloqueantes

### Obs 210 - `V2__seed_basico.sql` esta corrupto y rompe una base limpia

Problema: `V2__seed_basico.sql` tiene sentencias `ON CONFLICT` incrustadas donde no corresponden. En el seed de `parametro_sistema`, el `ON CONFLICT (clave) DO NOTHING` quedo dentro del texto de descripcion de `IMPUESTOS_MODO_AVANZADO`; por eso el `INSERT` termina sin clausula real de conflicto. En el seed de `entidad`, el comentario de "Gastos e imputaciones" quedo partido y la linea `ON CONFLICT (entidad, codigo) DO NOTHING; codigos del legado para el ETL)` queda como SQL suelto antes de continuar la lista de valores.

Impacto: una instalacion limpia con Flyway no puede aplicar correctamente las migraciones base. REQ-0011 depende justamente de `parametro_sistema`: si el seed inicial queda invalido o no idempotente, no se puede garantizar configuracion viva, SMTP, parametros de login ni `IMPUESTOS_MODO_AVANZADO` en ambientes nuevos o reconstruidos.

Solucion esperada: corregir `V2__seed_basico.sql` para que:

- el `INSERT INTO parametro_sistema ... VALUES (...)` cierre con `ON CONFLICT (clave) DO NOTHING;` fuera de las cadenas de texto;
- la descripcion de `IMPUESTOS_MODO_AVANZADO` sea un texto normal, sin salto que meta SQL adentro;
- el comentario de "Gastos e imputaciones" sea comentario valido;
- el `INSERT INTO entidad ... VALUES (...)` cierre con `ON CONFLICT (entidad, codigo) DO NOTHING;` despues de la ultima fila;
- se pruebe una ejecucion de migraciones en base limpia o un mecanismo equivalente que valide sintaxis/idempotencia Flyway.

Evidencia:

- `Desarrollo/sginmo-web/src/main/resources/db/migration/V2__seed_basico.sql:17`
- `Desarrollo/sginmo-web/src/main/resources/db/migration/V2__seed_basico.sql:18`
- `Desarrollo/sginmo-web/src/main/resources/db/migration/V2__seed_basico.sql:83`
- `Desarrollo/sginmo-web/src/main/resources/db/migration/V2__seed_basico.sql:84`

## Pruebas

- Revision estatica de service, bean, entidad, XHTML y migraciones.
- Build multi-modulo ejecutado desde `Desarrollo`:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-23'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& ..\herramientas\apache-maven-3.9.9\bin\mvn.cmd -q clean package
```

Resultado: EXIT 0.

No se aprueba porque Maven no valida la sintaxis de las migraciones SQL y el defecto bloqueante esta en Flyway/base limpia.
