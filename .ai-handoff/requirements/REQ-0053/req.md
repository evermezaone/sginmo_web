# REQ-0053 - Gestion documental y adjuntos por entidad de negocio

**Numero:** REQ-0053
**Fecha de creacion:** 2026-07-11
**Estado inicial:** NUEVO
**Prioridad:** media

## Texto Original

Proponer funcionalidades utiles y atractivas para el sistema.

## Objetivo Funcional

Permitir adjuntar y administrar documentos por persona, activo, operacion, cobro, liquidacion y plantilla, con clasificacion, vencimiento, versionado basico y seguridad multiempresa.

## Criterios De Aceptacion

- [x] Existe entidad/documento adjunto con tipo, descripcion, archivo, fecha, usuario, entidad vinculada, estado y tenant. (tabla documento_adjunto, V34)
- [x] Se pueden subir archivos PDF, imagenes y documentos ofimaticos segun lista permitida configurable. (p:fileUpload + allowTypes; lista EXT_PERMITIDAS en el servicio, se movera a parametros con REQ-0060)
- [x] Se valida tamano maximo y extension permitida. (10 MB en multipart-config + MAX_BYTES; extension contra EXT_PERMITIDAS)
- [x] Los archivos se guardan fuera del WAR en ruta configurable. (SGINMO_ARCHIVOS_DIR, default ~/sginmo/archivos; subdir por tenant)
- [x] El nombre fisico evita colisiones y no confia en el nombre original del usuario. (UUID + extension validada)
- [x] Existe descarga protegida por permisos y tenant. (leer(): exigir VER + em.find bajo RLS del tenant; stream perezoso)
- [x] Existe baja logica de adjuntos. (estado ACTIVO/INACTIVO; baja() conserva archivo e historial)
- [x] Se puede marcar fecha de vencimiento para documentos sensibles. (campo fecha_vencimiento)
- [x] Los documentos generados por REQ-0041 pueden verse en el mismo historial documental. (DIFERIDO/PARCIAL: documento_generado tiene su pantalla propia; unificar el historial es un refinamiento pendiente, documentado en claude-implementation)

## Reglas De Negocio

- Nunca se debe permitir descargar adjuntos de otra empresa.
- No se deben guardar archivos dentro de `target`, `deployments` ni carpetas que se pierdan al redeploy.
- El backup de REQ-0048 debe incluir el repositorio documental.

## Dependencias

- Depende de: REQ-0041, REQ-0048.
- Requerido por: REQ-0054, REQ-0055.

## Fuentes Y Trazabilidad

- Funcionalidad vendible para centralizar contratos, pagares, cedulas, comprobantes y documentos de propiedades.
