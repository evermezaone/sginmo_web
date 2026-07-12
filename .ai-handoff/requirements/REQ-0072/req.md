# REQ-0072 - Ocupacion, vacancia y brecha de alquiler

**Numero:** REQ-0072
**Fecha de creacion:** 2026-07-12
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

Del usuario (2026-07-12): "objetivo es llegar al 90 de ocupacion en propiedades dispuestos en alquiler... si dice falta 10 propiedades para alquilar y llegar al objetivo, que en un click se pueda ver esas propiedades."

## Objetivo Funcional

Agregar indicadores profundos de ocupacion/vacancia para propiedades alquilables, con brecha calculada
hacia objetivos de ocupacion y evidencia directa de las propiedades disponibles que explican esa brecha.

## Criterios De Aceptacion

- [ ] El sistema identifica el universo de activos alquilables: propiedades disponibles para alquiler segun tipo, estado, precio_alquiler, configuracion o regla definida.
- [ ] Calcula ocupacion: activos alquilables con operacion de alquiler vigente / total activos alquilables.
- [ ] Calcula vacancia: activos alquilables sin alquiler vigente.
- [ ] Calcula brecha contra objetivo: cantidad minima de propiedades a alquilar para llegar al porcentaje objetivo.
- [ ] Muestra ocupacion/vacancia por tipo de activo, sucursal/zona, propietario y rango de precio cuando existan datos.
- [ ] Permite abrir en un click las propiedades vacantes que faltan para cumplir el objetivo, ordenadas por prioridad comercial.
- [ ] Distingue activos no alquilables, vendidos, bloqueados, en mantenimiento o fuera de cartera para no contaminar la tasa.
- [ ] La regla de alquilable debe estar documentada y, si queda ambigua, configurable.
- [ ] Los calculos respetan tenant y no muestran activos de otra empresa.
- [ ] Los resultados deben alimentar REQ-0073 (objetivos) y REQ-0074 (evidencia).

## Reglas De Negocio

- Un activo con venta consumada no cuenta como alquilable.
- Un activo con operacion de alquiler vigente cuenta como ocupado aunque tenga cuotas vencidas.
- Un activo libre pero marcado como no disponible comercialmente no debe contar como vacante comercial si existe esa marca/configuracion.

## Dependencias

- Depende de: REQ-0013, REQ-0016, REQ-0069.
- Requerido por: REQ-0073, REQ-0074, REQ-0075.

## Fuentes Y Trazabilidad

- Pedido directo del usuario sobre ocupacion 90% y evidencia de propiedades faltantes.
