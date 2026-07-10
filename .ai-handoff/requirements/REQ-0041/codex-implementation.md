# REQ-0041 - Implementacion Codex

**Fecha:** 2026-07-10T15:51:09-03:00  
**Actor:** codex  
**Motivo:** el usuario indico que Claude no estaba activo y pidio desarrollar y auto-auditar este REQ.

## Implementado

- Migracion `V29__plantillas_documentos.sql`:
  - tablas `plantilla_documento` y `documento_generado`;
  - pantalla `plantillas-documentos`;
  - acciones nuevas `GENERAR_CONTRATO`, `GENERAR_PAGARE`, `DESCARGAR_DOCUMENTO`;
  - plantillas globales iniciales para contrato y pagare;
  - RLS por tenant/global.
- Entidades JPA:
  - `PlantillaDocumento`;
  - `DocumentoGenerado`.
- Servicio documental:
  - catalogo cerrado de variables;
  - validacion de placeholders desconocidos;
  - renderizado seguro de texto;
  - seleccion de plantilla activa por prioridad tenant > global y especifica > generica;
  - generacion PDF OpenPDF para contrato, pagare individual y lote de pagares;
  - registro de historial con plantilla, version y hash SHA-256.
- UI:
  - ABM `plantillas-documentos.xhtml`;
  - acceso desde menu;
  - botones en detalle de Operaciones para contrato, pagare por cuota y pagares pendientes/seleccionados.
- Pruebas unitarias:
  - `PlantillaDocumentoMotorTest`;
  - `NumeroLetrasTest`.

## Verificacion

- `mvn -q clean package` ejecutado desde `migracion\Desarrollo`: **EXIT 0**.
- No se uso JasperReports ni `.jrxml`; la salida generada es PDF con OpenPDF.
- No se hizo deploy en VPS en esta iteracion.
- No se hizo validacion visual con navegador; queda como verificacion recomendada post-deploy.
