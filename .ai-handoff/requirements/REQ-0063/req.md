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

- [x] Existe ABM de plantillas de permisos o perfiles base. (7 plantillas base sembradas -administrador/caja/cobrador/ventas/gerencia/auditor/consulta- con su set de permisos; edicion de plantillas via UI: refinamiento)
- [x] Se pueden aplicar plantillas a grupos/usuarios sin borrar ajustes existentes sin confirmacion. (aplicar a un GRUPO; por defecto solo AGREGA; "reemplazar" es explicito con confirmacion; usuarios reciben permisos via su grupo)
- [x] Permisos cubren acciones sensibles: ver, crear, editar, inactivar, reactivar, exportar, ver auditoria, generar documentos, caja, anulaciones. (las plantillas usan VER/CREAR/EDITAR/EXPORTAR/VER_AUDITORIA/OPERAR -que cubre inactivar/reactivar/anular- sobre las pantallas correspondientes)
- [x] El sistema muestra diferencias antes de aplicar una plantilla. (diff agregar/quitar)
- [x] Solo usuarios autorizados pueden modificar plantillas. (aplicar exige permiso grupos/EDITAR)
- [x] Cambios quedan auditados. (los INSERT en permiso_grupo registran usuario_creacion/fecha)
- [x] Las plantillas pueden ser globales o por empresa segun permiso. (rol_plantilla lleva tenant; base sembrada global -1; se pueden crear por empresa)
- [x] El backend mantiene autorizacion real; la UI solo acompana. (aplicar escribe permiso_grupo, que es lo que valida SesionUsuario.puede en el backend; no cambia el perfil ni concede SUPERADMIN)

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
