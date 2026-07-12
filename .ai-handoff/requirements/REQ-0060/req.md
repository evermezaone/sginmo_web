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

- [ ] Existe pantalla de parametros por empresa/sucursal con agrupacion clara.
- [ ] Los parametros tienen tipo de dato, descripcion, valor por defecto, validacion y alcance.
- [ ] Se documentan parametros iniciales: mora, caja obligatoria, dias de alerta contrato, limites de exportacion, logo, pie de comprobante y politica documental.
- [ ] Cambios de parametros sensibles quedan auditados.
- [ ] El sistema usa los parametros desde servicios, no desde constantes hardcodeadas.
- [ ] Hay cache o lectura eficiente sin desincronizar cambios criticos.
- [ ] Permiso separado para administrar parametros.
- [ ] Los parametros globales solo pueden ser editados por perfil autorizado.

## Reglas De Negocio

- Un parametro mal configurado no debe romper cobros, montos ni documentos; debe validarse antes de guardar.
- La configuracion tenant especifica tiene prioridad sobre default global.
- Los cambios deben afectar solo operaciones futuras salvo regla explicita.

## Dependencias

- Depende de: REQ-0011, REQ-0005.
- Requerido por: REQ-0052, REQ-0058, REQ-0059, REQ-0064.

## Fuentes Y Trazabilidad

- Estandar ABM: combos y parametros configurables desde BD.
