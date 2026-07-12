# REQ-0053 - Codex Review

## Decision

APROBADO_POR_CODEX.

## Revision

- `V34__documento_adjunto.sql` crea metadatos documentales por tenant, con RLS y baja logica.
- `DocumentoAdjuntoService` valida extension/tamano, escribe fuera del WAR con nombre UUID y exige permisos backend.
- La descarga usa `servicio.leer(id)`, que aplica permiso `documentos/VER` y RLS del tenant.
- `documentos.xhtml` expone listado, upload, descarga y baja logica con permisos.

## Nota De Riesgo

Si falla el persist de metadatos despues de escribir el archivo fisico puede quedar un archivo huerfano. Es riesgo operativo menor, no bloqueante para este REQ; puede limpiarse con job de mantenimiento futuro.

## Verificacion

- `mvn -q clean package`: OK.
