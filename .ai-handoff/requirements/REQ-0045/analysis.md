# REQ-0045 - Analisis

**Estado:** EN_ANALISIS
**Fecha:** 2026-07-11
**Analista:** Claude

## Analisis Funcional

El dialogo de alta/edicion de persona crecio con las pestanas (Datos, roles, etc.) y en
pantallas de menor alto los botones Guardar/Cancelar quedan por debajo del borde visible,
obligando a hacer scroll de todo el dialogo para confirmar o cancelar. Se busca que el pie
de botones quede fijo y que solo el cuerpo del formulario haga scroll interno.

## Analisis Tecnico

- Ajuste puramente de presentacion en `personas.xhtml`.
- Se envuelve el cuerpo del dialogo (el `p:tabView` con todas las pestanas) en un `div` con
  `max-height:60vh; overflow-y:auto; overflow-x:hidden` para que el contenido se desplace
  dentro de un area acotada.
- El pie de botones (Cancelar/Guardar) queda fuera del area con scroll, en un `div` con
  clase `pie-dialogo`, de modo que siempre sea visible.
- No cambia ningun backing bean, servicio ni consulta; el `actionListener` de guardar y las
  actualizaciones (`update`) siguen igual.

## Riesgos

| Riesgo | Probabilidad | Impacto | Mitigacion |
|---|---|---|---|
| El scroll interno recorta contenido en pantallas muy chicas | baja | bajo | `max-height` en `vh` se adapta al alto de viewport; el area es desplazable |
| Regresion visual en pestana de roles (dataTable ancho) | baja | bajo | `overflow-x:hidden` en el cuerpo; el dataTable ya maneja su propio ancho |

**Semaforo Codex:** bajo

## Preguntas Abiertas

- [ ] Ninguna.

## Impacto En Costos / LLM

- Aumenta tokens por mensaje: no
- Agrega llamadas extra al LLM: no
- Puede resolverse sin IA: si

## Impacto En Datos

- Requiere migracion: no
- Tablas/colecciones afectadas: ninguna

## Recomendacion

**Desarrollar** — riesgo bajo, cambio de CSS/layout sin impacto en datos ni logica.
