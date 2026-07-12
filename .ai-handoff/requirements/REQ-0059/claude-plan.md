# REQ-0059 - Plan De Implementacion

**Estado:** APROBADO_PARA_DESARROLLO
**Fecha:** 2026-07-12

## Estrategia

Extender la planilla existente (no recrear) con datos de arqueo. ArqueoService aparte para el cierre
controlado, reapertura y PDF; CajaService NO se toca. Pantalla de arqueo.

## Archivos A Modificar

| Archivo | Cambio |
|---|---|
| V40__caja_arqueo.sql | ALTER planilla + pantalla arqueo |
| dominio/operacion/Planilla.java | campos de arqueo |
| servicio/ArqueoService.java | NUEVO — resumen/cierre/reapertura/PDF |
| web/ArqueoBean.java + webapp/arqueo.xhtml | NUEVOS |
| WEB-INF/plantilla.xhtml + smoke | menu + cobertura |
| (transversal) *.xhtml nuevos | fix convertDateTime type |

## Pruebas Previstas

- [ ] Build OK
- [ ] V40 rollback + deploy + smoke (26/26)
- [ ] CajaService intacto (caja renderiza)

## Riesgos

- Dominio de caja: mitigado extendiendo la planilla y sin tocar CajaService.

## Cambios De Datos

V40 ALTER planilla + pantalla arqueo.
