# REQ-0067 - Plan De Implementacion

**Estado:** APROBADO_PARA_DESARROLLO
**Fecha:** 2026-07-12

## Estrategia

Infraestructura de auditoria funcional visible: tabla `auditoria_funcional` (de negocio, con RLS e
inmutable), servicio de registro/consulta con enmascarado de secretos, y pantalla filtrable con
permiso propio. Se cablea un ejemplo vivo (DESBLOQUEAR). La instrumentacion campo-a-campo de cada
maestro (boton Historial por ABM + captura antes/despues + motivo obligatorio en inactivacion) usa la
misma API y se documenta como rollout incremental para no desestabilizar los ABM ya auditados.

## Archivos A Modificar

| Archivo | Cambio |
|---|---|
| V46__auditoria_funcional.sql | NUEVO — tabla + RLS + pantalla auditoria |
| servicio/AuditoriaFuncionalService.java | NUEVO — registrar/registrarCambios/consultar/historial |
| web/AuditoriaBean.java + webapp/auditoria.xhtml | NUEVOS — consulta filtrable |
| servicio/SeguridadPoliticaService.java | ejemplo vivo: audita el DESBLOQUEAR |
| WEB-INF/plantilla.xhtml + smoke | menu + cobertura |

## Pruebas Previstas

- [ ] Build OK
- [ ] Deploy + Flyway V46 + smoke (auditoria renderiza)
- [ ] Registro (DESBLOQUEAR) + no-secretos + RLS por tenant

## Riesgos

- Toca un servicio de seguridad (agrega audit al desbloqueo): aditivo, dentro de la misma transaccion.

## Cambios De Datos

V46: tabla auditoria_funcional (RLS) + pantalla auditoria.
