# REQ-0060 - Plan De Implementacion

**Estado:** APROBADO_PARA_DESARROLLO
**Fecha:** 2026-07-12

## Estrategia

Extender parametro_sistema (clave/valor por tenant, -1=global) con tipo/grupo/default + seed. Servicio
ParametroConfig que resuelve el valor efectivo (empresa sobre global) con cache. Wire de AgendaService.

## Archivos A Modificar

| Archivo | Cambio |
|---|---|
| V41__parametros_avanzados.sql | ALTER + seed |
| dominio/catalogo/ParametroSistema.java | tipo/grupo/valor_defecto |
| servicio/ParametroConfig.java | NUEVO — lectura efectiva + cache |
| servicio/ParametroService.java | invalidar cache al guardar |
| servicio/AgendaService.java | leer AGENDA_DIAS_ALERTA |
| webapp/parametros.xhtml | columna Grupo |

## Pruebas Previstas

- [ ] Build OK
- [ ] V41 rollback (columnas + seed + override)
- [ ] Deploy + smoke
- [ ] Mal valor no rompe (default tolerante)

## Riesgos

- Config usada por servicios: mitigado con defaults tolerantes.

## Cambios De Datos

V41 ALTER parametro_sistema + seed de defaults globales.
