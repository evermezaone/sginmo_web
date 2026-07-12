# REQ-0045 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-11
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0045
- Tipo de cambio: UI (CSS/layout de dialogo)
- Riesgo: bajo
- Archivos clave:
  - `sginmo-web/src/main/webapp/personas.xhtml`: el cuerpo del dialogo (el `p:tabView` con
    todas las pestanas) queda envuelto en un `<div style="max-height:60vh; overflow-y:auto;
    overflow-x:hidden; padding-right:.5rem;">` (linea 78) que cierra antes del pie (linea 258);
    los botones Cancelar/Guardar pasan a un `<div class="pie-dialogo">` fijo (linea 259) fuera
    del area con scroll.
- Comandos probados:
  - `mvn -q clean package -DskipTests`: BUILD OK.
  - `python tools/smoke-test-vps.py`: 19/19 pantallas RENDER OK, incluida `personas`.
- Cambios de datos: no.
- Cambios de entorno: no.
- Impacto LLM/tokens: no.
- Decision esperada: aprobar (riesgo bajo, solo presentacion).
- Notas para auditor:
  - No se toco ningun backing bean ni servicio; el guardado y las validaciones de persona son
    identicos. El unico cambio es el envoltorio con scroll interno y el pie fijo.

## Resumen Funcional

El dialogo de alta/edicion de persona ahora muestra siempre los botones Guardar/Cancelar al
pie: el cuerpo del formulario (pestanas Datos y roles) hace scroll interno dentro de un area
de alto acotado, sin empujar los botones fuera de la vista.

## Resumen Tecnico

En `personas.xhtml` el contenido del `ui:fragment` (el `p:tabView` completo) se envuelve en un
`div` con `max-height:60vh; overflow-y:auto; overflow-x:hidden`. El pie con los
`p:commandButton` de Cancelar y Guardar queda en un `div.pie-dialogo` fuera de ese contenedor,
por lo que permanece visible sin importar el alto del formulario. Sin cambios en Java ni SQL.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| webapp/personas.xhtml | Envoltorio con scroll interno del cuerpo + pie de botones fijo (`pie-dialogo`) |

## Cambios De Datos

Sin cambios de datos. Sin migracion.

## Variables De Entorno

Ninguna.

## Pruebas Ejecutadas

Ver `test-plan.md`. Resumen: build OK, smoke-test 19/19 RENDER OK incluida `personas`.

## Pruebas Manuales Sugeridas

1. Login → Personas → Nuevo/Editar: con el dialogo abierto, comprobar que Guardar/Cancelar
   quedan al pie visibles y que el cuerpo hace scroll interno.
2. Editar una persona con muchos roles y confirmar que el guardado sigue funcionando.

## Riesgos Conocidos

- Ninguno relevante; en viewports muy bajos el area de 60vh es pequena pero desplazable.
