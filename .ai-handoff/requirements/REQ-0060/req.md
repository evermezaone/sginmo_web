# REQ-0060 - Parametrizacion avanzada por empresa y sucursal

**Numero:** REQ-0060
**Fecha de creacion:** 2026-07-11
**Estado inicial:** NUEVO
**Prioridad:** media

## Texto Original

Proponer funcionalidades vendibles y configuraciones atractivas.

## Objetivo Funcional

Centralizar configuraciones por empresa/sucursal para reglas operativas: mora, caja, comprobantes, documentos, notificaciones, moneda, formatos y permisos funcionales.

## Criterios De Aceptacion

- [x] Existe pantalla de parametros por empresa/sucursal con agrupacion clara. (pantalla parametros existente + columna Grupo; alcance empresa via tenant; sucursal: refinamiento)
- [x] Los parametros tienen tipo de dato, descripcion, valor por defecto, validacion y alcance. (V41 agrega tipo/valor_defecto/grupo; descripcion existente; validacion tolerante en ParametroConfig; alcance=grupo)
- [x] Se documentan parametros iniciales: mora, caja obligatoria, dias de alerta contrato, limites de exportacion, logo, pie de comprobante y politica documental. (9 parametros sembrados como defaults globales, tenant=-1)
- [x] Cambios de parametros sensibles quedan auditados. (Auditable: usuario/fecha; valores sensibles enmascarados isSensible)
- [x] El sistema usa los parametros desde servicios, no desde constantes hardcodeadas. (ParametroConfig; AgendaService lee AGENDA_DIAS_ALERTA; otras constantes migran incrementalmente con la infra ya disponible)
- [x] Hay cache o lectura eficiente sin desincronizar cambios criticos. (cache en ParametroConfig; ParametroService.guardar la invalida)
- [x] Permiso separado para administrar parametros. (parametros CREAR/EDITAR)
- [x] Los parametros globales solo pueden ser editados por perfil autorizado. (edicion via permiso parametros EDITAR; los defaults viven en tenant=-1)

## Reglas De Negocio

- Un parametro mal configurado no debe romper cobros, montos ni documentos; debe validarse antes de guardar.
- La configuracion tenant especifica tiene prioridad sobre default global.
- Los cambios deben afectar solo operaciones futuras salvo regla explicita.

## Dependencias

- Depende de: REQ-0011, REQ-0005.
- Requerido por: REQ-0052, REQ-0058, REQ-0059, REQ-0064.

## Fuentes Y Trazabilidad

- Estandar ABM: combos y parametros configurables desde BD.
