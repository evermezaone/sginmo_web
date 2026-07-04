# Categorias de Auditoria — Vocabulario Controlado

**USO OBLIGATORIO:** Tanto Claude como Codex deben usar EXCLUSIVAMENTE los codigos de esta tabla
al registrar observaciones via `POST /api/auditoria/observacion`.

Si el codigo no existe en esta lista → el SP rechaza el insert con error 45000.
Para agregar una nueva categoria: actualizar esta tabla + ejecutar INSERT en AUDITORIA_CATEGORIA.

## Codigos validos

| Codigo                       | Nombre                         | Cuando usar |
|------------------------------|--------------------------------|-------------|
| `test-plan-vacio`            | Test-plan vacio                | test-plan.md sin evidencia de pruebas reales (solo headings o texto generico) |
| `test-plan-impreciso`        | Test-plan con claims falsos    | Un CP afirma que existe un boton/endpoint/feature que no esta en el codigo |
| `trigger-bypass`             | Trigger bypaseado              | Saldo modificado con UPDATE directo en Python; debe gestionarse via trigger |
| `reversibilidad-incompleta`  | Reversibilidad incompleta      | La anulacion no restaura todos los saldos (Saldo, Estado, NC, etc.) via trigger |
| `encoding-documento`         | Encoding en documento          | Caracter especial (tilde, acento) que rompe regex o herramienta del pipeline |
| `criterio-sin-diferido`      | Criterio sin DIFERIDO formal   | Feature del req.md marcada como implementada ([x]) pero el codigo no la tiene, sin excepcion documentada |
| `comandos-no-documentados`   | Comandos no documentados       | claude-implementation.md sin evidencia de EXIT:0 en build/tsc/pytest |
| `build-error`                | Error de compilacion           | Error TypeScript (tsc) o Python (import/syntax) detectado |
| `archivo-corrupto`           | Archivo con contenido ajeno    | Scripts, basura de PowerShell u otro contenido appended a archivos .md o .py |
| `seguridad-backend`          | Seguridad backend ausente      | Endpoint sin validacion de permiso, o permiso equivocado |
| `sql-columna-inexistente`    | SQL referencia inexistente     | Query referencia columna, tabla o alias que no existe en el schema real |
| `estado-no-gestionado`       | Estado no gestionado           | Estado de negocio (DOCUMENTO.Estado, PEDIDO.Estado, etc.) que Python omite actualizar |
| `diferido-sin-justificacion` | DIFERIDO sin justificacion     | Feature pospuesta sin excepcion formal en Excepciones del test-plan o claude-implementation.md |
| `req-md-incompleto`          | req.md incompleto              | Criterios ambiguos, sin descripcion clara o sin casos de prueba asociables |

## Campo Subcategoria

Freeform VARCHAR(120). Convencion: nombre del elemento afectado en kebab-case o NombreTabla.Columna.

Ejemplos:
- `NC.Saldo` (para trigger-bypass sobre nota de credito)
- `boton-generar-factura` (para test-plan-impreciso)
- `PEDIDO.Estado` (para estado-no-gestionado)
- `Manifiesto-Minimo` (para encoding-documento)
- `claude-implementation.md` (para archivo-corrupto)

## API de referencia

- `GET  /api/auditoria/categorias`        — lista codigos + descripcion
- `POST /api/auditoria/observacion`       — registrar observacion
- `GET  /api/auditoria/observaciones`     — listar con filtros (req, categoria, auditor, etc.)
- `GET  /api/auditoria/estadisticas`      — agrupado por categoria (para analisis)
- `PATCH /api/auditoria/observacion/{id}` — marcar como corregido/aceptado/diferido
