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

- [x] El dashboard muestra alertas automaticas cuando un objetivo esta en riesgo o incumplido. (AlertaService.generar() deriva de los objetivos con semaforo != OK; pantalla Alertas + modulo)
- [x] Alertas iniciales: ocupacion bajo objetivo, mora sobre umbral, egresos sobre objetivo (via objetivos), rentabilidad negativa y contratos por vencer (reglas fijas). (cobros-bajo-mes-anterior queda como regla adicional documentada -el motor MoM ya lo detecta-)
- [x] Cada alerta muestra causa, impacto y accion sugerida. (columnas causa/impacto/accion_sugerida)
- [x] Cada alerta enlaza a evidencia de detalle (REQ-0074). (drill_clave -> dashboard-detalle; boton "ver evidencia" cuando aplica)
- [x] Las alertas se calculan con reglas configurables/parametros, no con numeros hardcodeados. (objetivos configurables + parametro CONTRATOS_AVISO_DIAS; umbrales vienen de los objetivos)
- [x] Se puede marcar una alerta como revisada/descartada con motivo y usuario. (cerrar(): DESCARTADA exige motivo; guarda usuario+fecha; auditado REQ-0067)
- [x] Las alertas no se duplican: misma entidad/indicador/periodo se consolida. (hash_dedup = tipo|indicador|periodo; unique index parcial sobre ABIERTA; upsert chequea existencia)
- [x] La UI muestra prioridades: critica, alta, media, informativa. (columna prioridad con tag por severidad; orden por prioridad)
- [x] Las alertas respetan tenant y permisos. (RLS V51; permiso alertas/VER para ver, alertas/EDITAR para cerrar)
- [x] Queda preparado el envio futuro por email/notificacion sin exigirlo. (modelo con causa/impacto/accion/prioridad listo; el envio es un paso futuro documentado, no implementado)

## Reglas De Negocio

- Una alerta debe ser accionable; si no puede abrir evidencia o explicar causa, no debe mostrarse como critica.
- Los objetivos configurados por REQ-0073 tienen prioridad sobre umbrales por defecto.
- Las alertas descartadas conservan historial para auditoria gerencial.

## Dependencias

- Depende de: REQ-0073, REQ-0074.
- Requerido por: seguimiento ejecutivo y control comercial.

## Fuentes Y Trazabilidad

- Pedido directo del usuario sobre seguimiento automatico de objetivos.
