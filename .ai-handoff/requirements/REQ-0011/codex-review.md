# Codex Review - REQ-0011

Fecha: 2026-07-07
Auditor: codex
Resultado: APROBADO

## Ronda 2

Se reaudito la correccion de la Obs 210 sobre `V2__seed_basico.sql`.

Verificado correcto:

- `IMPUESTOS_MODO_AVANZADO` tiene descripcion en una sola cadena normal.
- El `INSERT INTO parametro_sistema ... VALUES (...)` cierra con `ON CONFLICT (clave) DO NOTHING;` fuera de strings.
- El comentario de "Gastos e imputaciones" es comentario SQL valido.
- El `INSERT INTO entidad ... VALUES (...)` cierra con `ON CONFLICT (entidad, codigo) DO NOTHING;` despues de la ultima fila.
- No quedan lineas `ON CONFLICT` incrustadas dentro de descripciones o comentarios partidos en `V2__seed_basico.sql`.

Archivos revisados:

- `Desarrollo/sginmo-web/src/main/resources/db/migration/V2__seed_basico.sql`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/dominio/catalogo/ParametroSistema.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/servicio/ParametroService.java`
- `Desarrollo/sginmo-web/src/main/java/py/com/pysistemas/sginmo/web/ParametroBean.java`
- `Desarrollo/sginmo-web/src/main/webapp/parametros.xhtml`

## Observaciones

Sin hallazgos bloqueantes pendientes.

Obs 210 queda corregida: la migracion base ya no contiene `ON CONFLICT` dentro de strings/comentarios y conserva idempotencia declarada para los seeds de parametros y entidades.

## Pruebas

- Revision estatica de migracion, service, bean, entidad y XHTML.
- Build multi-modulo ejecutado desde `Desarrollo`:

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-23'
$env:Path="$env:JAVA_HOME\bin;$env:Path"
& ..\herramientas\apache-maven-3.9.9\bin\mvn.cmd -q clean package
```

Resultado: EXIT 0.
