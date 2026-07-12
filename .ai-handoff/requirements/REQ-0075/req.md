# REQ-0075 - Alertas gerenciales, brechas y recomendaciones de accion

**Numero:** REQ-0075
**Fecha de creacion:** 2026-07-12
**Estado inicial:** NUEVO
**Prioridad:** media

## Texto Original

Del usuario (2026-07-12): "fijacion de objetivos con deteccion automatica de valores... poder hacer seguimiento de esos objetivos."

## Objetivo Funcional

Agregar una capa de alertas gerenciales que detecte desviaciones, brechas y oportunidades accionables
desde las metricas y objetivos del dashboard.

## Criterios De Aceptacion

- [ ] El dashboard muestra alertas automaticas cuando un objetivo esta en riesgo, incumplido o mejora/empeora significativamente.
- [ ] Alertas iniciales: ocupacion bajo objetivo, mora sobre umbral, cobros por debajo del mes anterior, egresos por encima del objetivo, rentabilidad negativa y contratos por vencer.
- [ ] Cada alerta muestra causa, impacto y accion sugerida.
- [ ] Cada alerta enlaza a evidencia de detalle (REQ-0074).
- [ ] Las alertas se calculan con reglas configurables o parametros, no con numeros hardcodeados.
- [ ] Se puede marcar una alerta como revisada/descartada con motivo y usuario.
- [ ] Las alertas no deben duplicarse innecesariamente: misma entidad/indicador/periodo debe consolidarse.
- [ ] La UI muestra prioridades: critica, alta, media, informativa.
- [ ] Las alertas respetan tenant y permisos.
- [ ] Debe quedar preparado el envio futuro por email/notificacion, sin exigirlo en esta primera version.

## Reglas De Negocio

- Una alerta debe ser accionable; si no puede abrir evidencia o explicar causa, no debe mostrarse como critica.
- Los objetivos configurados por REQ-0073 tienen prioridad sobre umbrales por defecto.
- Las alertas descartadas conservan historial para auditoria gerencial.

## Dependencias

- Depende de: REQ-0073, REQ-0074.
- Requerido por: seguimiento ejecutivo y control comercial.

## Fuentes Y Trazabilidad

- Pedido directo del usuario sobre seguimiento automatico de objetivos.
