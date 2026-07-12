# REQ-0063 - Plan De Implementacion

**Estado:** APROBADO_PARA_DESARROLLO
**Fecha:** 2026-07-12

## Estrategia

Plantillas = set de (pantalla, accion). Aplicar a un grupo escribe permiso_grupo (autorizacion real).
Diff antes de aplicar. 7 perfiles base sembrados. Nunca toca el perfil del usuario.

## Archivos A Modificar

| Archivo | Cambio |
|---|---|
| V44__roles_plantilla.sql | 2 tablas + seed + RLS + pantalla |
| servicio/RolPlantillaService.java | NUEVO — diff/aplicar |
| web/RolPlantillaBean.java + webapp/roles-plantilla.xhtml | NUEVOS |
| WEB-INF/plantilla.xhtml + smoke | menu + cobertura |

## Pruebas Previstas

- [ ] Build OK
- [ ] V44 rollback (tablas + seed + pantalla)
- [ ] Deploy + smoke
- [ ] Autorizacion real (permiso_grupo) + no superadmin

## Riesgos

- Seguridad: mitigado (escribe permiso_grupo, no perfil; aislamiento por tenant).

## Cambios De Datos

V44 crea rol_plantilla + rol_plantilla_permiso + seed + pantalla.
