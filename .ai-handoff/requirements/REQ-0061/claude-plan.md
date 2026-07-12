# REQ-0061 - Plan De Implementacion

**Estado:** APROBADO_PARA_DESARROLLO
**Fecha:** 2026-07-12

## Estrategia

Framework generico CSV (parse/preview/validar/importar atomico) + tabla de historial. Cada tipo es un
mapper que reusa el service de negocio. Tipo de referencia: PARAMETRO. XLSX diferido (POI no aprobado).

## Archivos A Modificar

| Archivo | Cambio |
|---|---|
| V42__importacion.sql | tabla historial + RLS + pantalla |
| servicio/ImportacionService.java | NUEVO — framework CSV |
| web/ImportacionBean.java + webapp/importacion.xhtml | NUEVOS |
| WEB-INF/plantilla.xhtml + smoke | menu + cobertura |

## Pruebas Previstas

- [ ] Build OK
- [ ] V42 rollback + deploy + smoke
- [ ] Atomico + reuso de validaciones

## Riesgos

- Importa datos: mitigado por atomicidad y reuso del service.

## Cambios De Datos

V42 tabla importacion + pantalla.
