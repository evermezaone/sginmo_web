# REQ-0054 - Plan De Implementacion

**Estado:** APROBADO_PARA_DESARROLLO
**Fecha:** 2026-07-12

## Estrategia

Extender `documento_generado` (REQ-0041, ya con RLS V29) con estado documental + fechas de ciclo +
adjunto firmado (id de documento_adjunto de REQ-0053) + anulacion. Pantalla nueva de gestion.

## Archivos A Modificar

| Archivo | Cambio |
|---|---|
| V35__documento_estado.sql | ALTER documento_generado + pantalla |
| dominio/documento/DocumentoGenerado.java | nuevos campos |
| servicio/DocumentoGeneradoService.java | NUEVO — estado/firma/anulacion |
| web/DocumentoGeneradoBean.java | NUEVO |
| webapp/documentos-generados.xhtml | NUEVO |
| WEB-INF/plantilla.xhtml + smoke | menu + cobertura |

## Pruebas Previstas

- [ ] Build OK
- [ ] V35 en rollback (columnas + pantalla)
- [ ] Deploy + Flyway V35 + smoke
- [ ] Permisos: cambiar estado (EDITAR) vs anular (INACTIVAR)

## Riesgos

- Documentos legales: transiciones de estado y anulacion. Anular no borra archivo/historial.

## Cambios De Datos

V35 ALTER `documento_generado` + registra pantalla `documentos-generados`.
