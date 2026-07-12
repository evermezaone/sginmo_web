# REQ-0053 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0053
- Tipo de cambio: BD (tabla + RLS) + backend + UI + configuracion (web.xml multipart)
- Riesgo: medio-alto (manejo de archivos en disco + descarga protegida por tenant + config multipart global)
- Archivos clave:
  - `WEB-INF/web.xml`: `multipart-config` (10 MB) + `primefaces.UPLOADER=native`.
  - `V34__documento_adjunto.sql`: tabla por-tenant + RLS inline + pantalla `documentos`.
  - `dominio/documento/DocumentoAdjunto.java`: entidad (Auditable, estados varchar+CHECK).
  - `servicio/DocumentoAdjuntoService.java`: guardar (valida ext/tamano, UUID, escribe en disco), leer (permiso VER + RLS), baja logica, listar lazy. Ruta base `SGINMO_ARCHIVOS_DIR` (default ~/sginmo/archivos), subdir por tenant.
  - `web/DocumentoBean.java`: @ViewScoped, LazyDataModel, upload (UploadedFile), descarga StreamedContent perezoso.
  - `webapp/documentos.xhtml`: lista + dialogo (upload simple, ajax=false) + descarga + baja.
  - `WEB-INF/plantilla.xhtml`, `tools/smoke-test-vps.py`: menu + cobertura.
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - V34 en `BEGIN...ROLLBACK`: tabla + RLS + pantalla + insert de prueba OK (1 fila).
  - Deploy + Flyway V34 `success=t`; `python tools/smoke-test-vps.py`: 21/21 RENDER OK incluida `documentos`.
- Cambios de datos: si, V34 crea `documento_adjunto` (vacia) + pantalla.
- Cambios de entorno: `SGINMO_ARCHIVOS_DIR` (opcional; default ~/sginmo/archivos, ya respaldada por REQ-0065).
- Impacto LLM/tokens: no.
- Decision esperada: revisar riesgo puntual (escritura/lectura de archivos, aislamiento de descarga por tenant, config multipart) + limitacion documentada del criterio 9.
- Notas para auditor:
  - Descarga: `leer()` exige VER y hace `em.find` bajo RLS -> solo el tenant duenio ve el registro; el path fisico se compone con `d.getTenant()`. Verificar que no hay path traversal (nombre_fisico es UUID+ext generado, no input del usuario).
  - Archivos fuera del WAR (no en target/deployments): sobreviven al redeploy y entran al backup (REQ-0065).
  - Validacion de extension (whitelist) + tamano (multipart 10 MB + MAX_BYTES).

## Resumen Funcional

Nueva pantalla "Documentos" (menu Operaciones): adjuntar archivos (PDF/imagenes/office) vinculados a
una entidad de negocio (persona/activo/operacion/cobro/liquidacion/plantilla/general), con tipo,
descripcion y vencimiento opcional; listar, descargar (protegido) y dar de baja (logica).

## Resumen Tecnico

`documento_adjunto` por-tenant con RLS. El servicio valida extension/tamano, genera nombre fisico UUID,
escribe en `SGINMO_ARCHIVOS_DIR/<tenant>/` y persiste metadatos. La descarga lee el archivo solo tras
verificar permiso VER y pertenencia por tenant (RLS). Upload nativo de PrimeFaces (multipart-config).

## Limitaciones Conocidas (transparencia)

- Criterio 9 (documentos generados de REQ-0041 en el MISMO historial): DIFERIDO. `documento_generado`
  tiene su pantalla propia (Plantillas/REQ-0041); unificar ambos historiales en la vista de Documentos
  es un refinamiento pendiente (requiere DTO/union de dos tablas de forma distinta).
- Lista de extensiones y tamano maximo: hoy en constantes del servicio; se movera a parametros (REQ-0060).
- Vinculo por `entidad_tipo` + `entidad_id` (id numerico); selector navegable de la entidad concreta es refinamiento.
- Prueba funcional de upload/descarga real: pendiente de verificacion manual del usuario (el smoke solo cubre render).

## Archivos Modificados

Ver Manifiesto. Migracion V34 nueva.

## Cambios De Datos

V34: crea `documento_adjunto` (vacia) con RLS per-tenant + indice por entidad; registra pantalla `documentos`.

## Variables De Entorno

- `SGINMO_ARCHIVOS_DIR` (opcional): raiz del repositorio documental. Default `~/sginmo/archivos`.

## Pruebas Ejecutadas

Ver `test-plan.md`. Build OK; V34 validada en rollback; deploy + Flyway success; smoke 21/21.

## Pruebas Manuales Sugeridas

1. Documentos -> Adjuntar: subir un PDF vinculado a una persona; verlo en la lista; descargarlo; darlo de baja.
2. Intentar subir una extension no permitida o >10 MB -> rechazo con mensaje.

## Riesgos Conocidos

- Escritura en disco del servidor (ruta configurable, fuera del WAR).
- Ver "Limitaciones Conocidas".
