# Codex Review - REQ-0016

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-07
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

- Obs 216: `OperacionService` genera documentos, detalles y cuotas con `usuario_creacion = 'sistema'` o `p_usuario = 'sistema'`. En operaciones/cuentas corrientes el audit trail debe conservar el usuario real que confirma la operacion; hardcodear `sistema` deja sin trazabilidad los documentos financieros y el cronograma.
- Obs 217: el detalle de operacion en `OperacionBean.ver()` busca la operacion recargando `operacionService.listar(0, 1000, "")` y filtrando en memoria. En una grilla lazy, cualquier operacion fuera de las primeras 1000 filas puede abrir el dialogo sin entidad seleccionada aunque la fila exista en la pagina visible.

### No Bloqueantes

- La verificacion visual de la pantalla sigue pendiente en el handoff de Claude. No se toma como bloqueante porque la revision actual ya encuentra fallas funcionales previas.

## Riesgos

- Auditoria financiera incompleta en documentos y cuotas generados por alta de operacion.
- La pantalla no escala de forma consistente con el estandar lazy real cuando el volumen supera las primeras 1000 operaciones.

## Pruebas Revisadas

- [x] Revision estatica de `OperacionService`.
- [x] Revision estatica de `OperacionBean`.
- [x] Revision estatica de `operaciones.xhtml`.
- [x] Revision estatica de `V16__motor_documento.sql`.

## Pruebas Faltantes

- [ ] Reejecutar `mvn -q package` luego de corregir.
- [ ] Prueba funcional de alta de operacion verificando usuario real en `documento`, `documento_detalle` y `cronograma_cuota`.
- [ ] Prueba funcional de detalle sobre una operacion que no pertenezca a las primeras 1000 filas ordenadas por id descendente.
