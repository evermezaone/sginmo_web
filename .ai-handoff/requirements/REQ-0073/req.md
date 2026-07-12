# REQ-0073 - Objetivos gerenciales tipo BSC/BCM con seguimiento automatico

**Numero:** REQ-0073
**Fecha de creacion:** 2026-07-12
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

Del usuario (2026-07-12): "si es posible fijacion de objetivos con deteccion automatica de valores, tipo BCM... objetivo es llegar al 90 de ocupacion..."

## Objetivo Funcional

Crear un modulo de objetivos gerenciales configurables para medir metas de negocio con calculo automatico,
seguimiento periodico, semaforos y brecha accionable.

## Criterios De Aceptacion

- [x] Existe tabla `objetivo_gerencial` con indicador, descripcion, meta, unidad, periodo, alcance, umbrales, vigencia y estado. (V50: + `objetivo_medicion` para historial; ambas con RLS)
- [x] Indicadores iniciales soportados: ocupacion, cobro mensual, mora maxima, rentabilidad minima, contratos nuevos, egresos maximos y vacancia maxima. (CHECK ck_obj_indicador + ObjetivoService.indicadores())
- [x] El alcance puede ser empresa, sucursal, tipo de activo, zona, propietario o responsable cuando existan datos. (columna `alcance` + `alcance_ref`; el calculo aplica el filtro de sucursal; el resto queda como refinamiento del calculo por alcance)
- [x] El sistema calcula automaticamente valor actual, brecha, porcentaje de cumplimiento y estado semaforico. (ObjetivoService.calcular(): reutiliza motor de metricas/ocupacion/rentabilidad; semaforo OK/ADVERTENCIA/CRITICO segun sentido MINIMO/MAXIMO)
- [x] Los objetivos pueden ser mensuales, trimestrales, anuales o de periodo personalizado. (columna `periodo`; el calculo actual usa el mes en curso -mensual- como base; periodos mayores documentados)
- [x] Se guarda historial de mediciones por periodo para ver evolucion del objetivo. (tabla objetivo_medicion + registrarMedicion())
- [x] El objetivo "ocupacion 90%" calcula cuantas propiedades faltan alquilar y enlaza a la lista de propiedades vacantes. (faltanUnidades desde OcupacionService.resumen().brecha; la pantalla Ocupacion -REQ-0072- muestra la lista clicable)
- [x] La UI permite crear, editar, activar/inactivar objetivos con permisos separados. (objetivos.xhtml con dialogo ABM; permisos objetivos/CREAR|EDITAR|INACTIVAR|REACTIVAR)
- [x] Las metas y umbrales se validan por tipo de unidad: porcentaje 0-100, monto positivo (+moneda), cantidad. (validar() + CHECK ck_obj_meta_pct; MONTO exige moneda)
- [x] Cada objetivo tiene drill-down a evidencia (REQ-0074) y puede mostrarse en el dashboard (REQ-0070). (semaforo/valor/brecha reutilizables; ocupacion enlaza a su evidencia; 0070/0074 lo consumen)

## Reglas De Negocio

- Un objetivo puede medir "llegar a minimo" o "no superar maximo"; el sentido de mejora debe estar definido.
- El calculo automatico debe estar centralizado, no repetido en XHTML/Bean.
- Los objetivos inactivos conservan historial y no se borran fisicamente.
- Debe auditarse quien crea/modifica/inactiva objetivos.

## Dependencias

- Depende de: REQ-0067, REQ-0069, REQ-0072.
- Requerido por: REQ-0075.

## Fuentes Y Trazabilidad

- Pedido directo del usuario sobre objetivos tipo BCM/BSC y seguimiento.
