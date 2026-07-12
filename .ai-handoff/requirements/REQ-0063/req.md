# REQ-0063 - Plantillas de roles y permisos por perfil operativo

**Numero:** REQ-0063
**Fecha de creacion:** 2026-07-11
**Estado inicial:** NUEVO
**Prioridad:** media

## Texto Original

Proponer funcionalidades utiles para elevar el nivel del sistema.

## Objetivo Funcional

Crear plantillas de roles/permisos para configurar rapidamente perfiles operativos: administrador, caja, cobrador, ventas, gerencia, auditor y solo consulta.

## Criterios De Aceptacion

- [ ] Existe ABM de plantillas de permisos o perfiles base.
- [ ] Se pueden aplicar plantillas a grupos/usuarios sin borrar ajustes existentes sin confirmacion.
- [ ] Permisos cubren acciones sensibles: ver, crear, editar, inactivar, reactivar, exportar, ver auditoria, generar documentos, caja, anulaciones.
- [ ] El sistema muestra diferencias antes de aplicar una plantilla.
- [ ] Solo usuarios autorizados pueden modificar plantillas.
- [ ] Cambios quedan auditados.
- [ ] Las plantillas pueden ser globales o por empresa segun permiso.
- [ ] El backend mantiene autorizacion real; la UI solo acompana.

## Reglas De Negocio

- No basta con ocultar botones; los servicios/endpoints deben validar permisos.
- Aplicar plantilla no debe conceder permisos de superadmin por accidente.
- Debe existir forma de revisar rapidamente que permisos tiene un usuario.

## Dependencias

- Depende de: REQ-0004, REQ-0010.
- Requerido por: implementacion en clientes y soporte.

## Fuentes Y Trazabilidad

- Estandar ABM: permisos por accion.
- Bugs que no se deben replicar: autorizacion solo UI.
