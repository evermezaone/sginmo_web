# REQ-0044 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0044
- Tipo de cambio: UI
- Riesgo: bajo
- Archivos clave:
  - `sginmo-web/src/main/webapp/personas.xhtml`: se elimino el `div.campo` con label "Clasificacion fiscal" y su `p:inputText value="#{personaBean.datosEmpresa.clasificacionFiscal}"` de la seccion de datos de empresa.
- Comandos probados:
  - `mvn -q clean package -DskipTests`: BUILD OK.
  - `python tools/smoke-test-vps.py`: 19/19 pantallas RENDER OK, incluida `personas`.
- Cambios de datos: no (la columna `persona_empresa.clasificacion_fiscal` queda deprecada, no se dropea).
- Cambios de entorno: no.
- Impacto LLM/tokens: no.
- Decision esperada: aprobar (cambio de UI de bajo riesgo).
- Notas para auditor:
  - No se toco dominio ni servicio; el getter/setter `clasificacionFiscal` puede quedar (columna viva en BD).
  - El cambio viajo en el mismo commit que REQ-0043/0045 (`2ec6672`), pero es independiente: solo remueve un bloque de UI.

## Resumen Funcional

El formulario de alta/edicion de Persona ya no muestra el campo "Clasificacion fiscal", que no
aplica en Paraguay. El resto del ABM (persona fisica/juridica, roles, contacto) sigue igual.

## Resumen Tecnico

Se removio de `personas.xhtml` el bloque `div.campo` que contenia el label "Clasificacion
fiscal" y el `p:inputText` ligado a `#{personaBean.datosEmpresa.clasificacionFiscal}`. No hay
cambios en `PersonaEmpresa`, `PersonaBean` ni en BD: la columna `clasificacion_fiscal`
permanece (deprecada) y la vista `v_persona` la sigue proyectando.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| webapp/personas.xhtml | eliminado el campo/label "Clasificacion fiscal" del ABM de Persona |

## Cambios De Datos

Sin cambios. La columna `persona_empresa.clasificacion_fiscal` queda deprecada (no se dropea);
no hay migracion.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Ver `test-plan.md`. Resumen: `mvn -q clean package -DskipTests` BUILD OK; smoke-test 19/19
RENDER OK incluida `personas`.

## Pruebas Manuales Sugeridas

1. Personas -> nueva/editar: confirmar que ya no aparece el campo "Clasificacion fiscal".
2. Guardar un alta y una edicion de persona: debe funcionar sin errores.

## Riesgos Conocidos

Ninguno. La columna sigue en BD por si se necesita el dato historico.
