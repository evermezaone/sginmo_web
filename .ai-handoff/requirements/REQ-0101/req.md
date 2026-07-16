# REQ-0101 - Portal login: igualar ancho del campo contrasena

**Numero:** REQ-0101
**Fecha de creacion:** 2026-07-16
**Estado inicial:** NUEVO
**Prioridad:** baja (estetica)

## Texto Original
"Igualar el campo password en su longitud a los demas campos (estetica)."

## Objetivo Funcional
El campo Contrasena del login del portal debe ocupar el mismo ancho que Documento (se veia mas angosto).

## Alcance
- WEB-INF/portal-acceso.xhtml: CSS .campo-a .ui-password { display:block; width:100% } + input width:100%.
  El p:password con toggleMask envuelve el input en un span inline que no tomaba el 100%.

## Criterios De Aceptacion
- [x] El campo Contrasena se ve del mismo ancho que Documento.
- [x] El toggle de ver/ocultar sigue funcionando.

## Dependencias
- Base: WEB-INF/portal-acceso.xhtml (template de login del portal).
