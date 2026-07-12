# REQ-0049 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0049
- Tipo de cambio: UI (solo marcado/estilos del dialogo de alta de Operacion)
- Riesgo: bajo (no toca backend, validacion ni submit)
- Archivos clave:
  - `sginmo-web/src/main/webapp/operaciones.xhtml`: el cuerpo del `p:dialog` de alta/edicion se envuelve en un `div` con scroll interno (`max-height:60vh; overflow-y:auto`) y los botones quedan en un `div.pie-dialogo` fuera del area desplazable. Mismo patron que REQ-0045 en `personas.xhtml`.
- Comandos probados:
  - `mvn -q clean package -DskipTests`: BUILD OK.
  - Deploy a la VPS (WAR + `.dodeploy`): OK.
  - `python tools/smoke-test-vps.py`: 19/19 pantallas RENDER OK (incluida `operaciones`).
- Cambios de datos: no.
- Cambios de entorno: no.
- Impacto LLM/tokens: no.
- Decision esperada: aprobar (cambio de UI de bajo riesgo).
- Notas para auditor:
  - No se modifico el `commandButton` "Registrar operacion" ni su `actionListener="#{operacionBean.crear()}"` ni los `update`.
  - El alto del cuerpo es relativo (`60vh`) y el dialogo es `responsive="true"`, por lo que funciona en escritorio y celular.

## Resumen Funcional

En el alta/edicion de Operacion, los botones Cancelar y "Registrar operacion" quedan siempre
visibles al pie del dialogo; el formulario, si es largo, se desplaza dentro de su propio cuerpo.

## Resumen Tecnico

Se envolvio el contenido del formulario del `p:dialog` en un `div` con `max-height:60vh; overflow-y:auto;
overflow-x:hidden`, y se movieron los botones a un `div.pie-dialogo` posterior al cuerpo desplazable.
Reutiliza el patron introducido por REQ-0045 (Persona).

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| webapp/operaciones.xhtml | wrapper con scroll interno + `div.pie-dialogo` con los botones |

## Cambios De Datos

Sin cambios.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Ver `test-plan.md`. Resumen: build OK, deploy OK, smoke-test 19/19 RENDER OK.

## Pruebas Manuales Sugeridas

1. Abrir el alta de Operacion en pantalla chica: los botones Cancelar/Registrar deben verse sin scrollear.
2. Cargar y registrar una operacion: la validacion y el submit deben seguir funcionando igual.

## Riesgos Conocidos

Ninguno relevante (cambio de UI aislado).
