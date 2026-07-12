# REQ-0044 - Analisis

**Estado:** EN_ANALISIS
**Fecha:** 2026-07-12
**Analista:** Claude

## Analisis Funcional

El ABM de Persona muestra un campo de texto "Clasificacion fiscal" que no tiene uso en el
regimen tributario de Paraguay. Se pide retirarlo de la interfaz para simplificar el
formulario. Es un cambio de UI puro: no hay logica de negocio que lea ese campo para calculos,
cobros ni reportes, por lo que quitarlo del formulario no afecta ningun flujo.

Se decide conservar la columna `persona_empresa.clasificacion_fiscal` en la base (queda
deprecada) para no perder datos historicos y evitar una migracion destructiva innecesaria.

## Analisis Tecnico

- `personas.xhtml`: se elimina el bloque `div.campo` con el label "Clasificacion fiscal" y su
  `p:inputText value="#{personaBean.datosEmpresa.clasificacionFiscal}"`.
- El getter/setter `clasificacionFiscal` en `PersonaEmpresa` puede quedar (columna deprecada);
  no se toca dominio ni servicio.
- Sin migracion Flyway: la columna permanece en BD y en la vista `v_persona`.

## Riesgos

| Riesgo | Probabilidad | Impacto | Mitigacion |
|---|---|---|---|
| Romper el render de personas al quitar el bloque | baja | bajo | smoke-test de render post-deploy (19/19) |
| Perder datos historicos de clasificacion fiscal | baja | bajo | no se dropea la columna; solo se retira de la UI |

**Semaforo Codex:** bajo

## Preguntas Abiertas

- [ ] Ninguna.

## Impacto En Costos / LLM

- Aumenta tokens por mensaje: no
- Agrega llamadas extra al LLM: no
- Puede resolverse sin IA: si

## Impacto En Datos

- Requiere migracion: no
- Tablas/colecciones afectadas: ninguna (la columna `clasificacion_fiscal` queda deprecada, sin cambios de datos)

## Recomendacion

**Desarrollar** — riesgo bajo, cambio de UI sin impacto en datos ni logica de negocio.
