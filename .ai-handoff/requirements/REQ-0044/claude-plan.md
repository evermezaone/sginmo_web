# REQ-0044 - Plan De Implementacion

**Estado:** APROBADO_PARA_DESARROLLO
**Fecha:** 2026-07-12

## Estrategia

Retirar de `personas.xhtml` el bloque de UI del campo "Clasificacion fiscal". No se toca
dominio, servicio ni BD: la columna queda deprecada. Verificar con build + smoke-test de
render que el ABM de Persona sigue sano.

## Archivos A Modificar

| Archivo | Cambio |
|---|---|
| webapp/personas.xhtml | eliminar el `div.campo` con label "Clasificacion fiscal" y su `p:inputText` |

## Pruebas Previstas

- [x] `mvn -q clean package -DskipTests` BUILD OK.
- [x] `python tools/smoke-test-vps.py` 19/19 RENDER OK incluida `personas`.

## Riesgos

Cambio de UI de bajo riesgo; unico riesgo es un error de markup que rompa el render, cubierto
por el smoke-test.

## Cambios De Datos

Sin cambios. La columna `persona_empresa.clasificacion_fiscal` queda deprecada (no se dropea).
