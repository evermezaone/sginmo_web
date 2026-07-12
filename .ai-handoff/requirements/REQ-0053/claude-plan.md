# REQ-0053 - Plan De Implementacion

**Estado:** APROBADO_PARA_DESARROLLO
**Fecha:** 2026-07-12

## Estrategia

Tabla `documento_adjunto` por-tenant (RLS inline V34). Archivos fisicos fuera del WAR en ruta
configurable (`SGINMO_ARCHIVOS_DIR`, la misma que respalda REQ-0065), subdir por tenant, nombre UUID.
Upload con `p:fileUpload` nativo (multipart-config en web.xml), descarga con `StreamedContent`
perezoso y chequeo permiso+tenant. Baja logica. Validacion de extension/tamano.

## Archivos A Modificar

| Archivo | Cambio |
|---|---|
| WEB-INF/web.xml | multipart-config + primefaces.UPLOADER=native |
| V34__documento_adjunto.sql | NUEVO — tabla + RLS + pantalla |
| dominio/documento/DocumentoAdjunto.java | NUEVO — entidad |
| servicio/DocumentoAdjuntoService.java | NUEVO — guardar/leer/baja/listar |
| web/DocumentoBean.java | NUEVO — bean lazy + upload + download |
| webapp/documentos.xhtml | NUEVO — vista |
| WEB-INF/plantilla.xhtml | item de menu Documentos |
| tools/smoke-test-vps.py | cobertura documentos |

## Pruebas Previstas

- [ ] Build OK
- [ ] V34 en rollback (tabla+RLS+pantalla+insert)
- [ ] Backup previo (esquema nuevo)
- [ ] Deploy + Flyway V34 success + smoke incluye documentos
- [ ] Aislamiento por tenant en descarga

## Riesgos

- Configuracion multipart (web.xml) afecta a toda la app: solo agrega parsing multipart, no rompe forms normales.
- Archivos en disco del servidor: ruta fuera del WAR para no perderse en redeploy; incluida en el backup (REQ-0065).

## Cambios De Datos

V34 crea tabla `documento_adjunto` + RLS + registra pantalla `documentos`.
