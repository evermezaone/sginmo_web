# REQ-0045 - Plan De Implementacion

**Estado:** APROBADO_PARA_DESARROLLO
**Fecha:** 2026-07-11

## Estrategia

Envolver el cuerpo del dialogo de persona en un contenedor con alto maximo y scroll vertical
interno, dejando el pie de botones (Guardar/Cancelar) fuera de ese contenedor para que quede
siempre visible. Sin tocar backend ni datos.

## Archivos A Modificar

| Archivo | Cambio |
|---|---|
| webapp/personas.xhtml | Envolver el `p:tabView` en un `div` con `max-height:60vh; overflow-y:auto`; pie de botones en `div.pie-dialogo` fijo |

## Pruebas Previstas

- [x] Build OK (`mvn clean package`).
- [x] Render de `personas` OK en smoke-test post-deploy.
- [x] Verificacion visual: botones al pie visibles con scroll interno.

## Riesgos

Bajo: solo CSS/layout. Sin migracion, sin cambios de logica.

## Cambios De Datos

Sin cambios.
