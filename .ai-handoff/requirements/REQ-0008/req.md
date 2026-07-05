# REQ-0008 - Catalogos: monedas, formas de pago e impuestos

**Numero:** REQ-0008
**Fecha de creacion:** 2026-07-04
**Estado inicial:** NUEVO
**Prioridad:** no indicada

## Texto Original

[Copiar el mensaje exacto del usuario aqui]

## Decisiones del usuario ya tomadas (2026-07-05 — incorporar al desarrollar este REQ)

1. **Impuestos con base imponible parcial**: la tabla `impuesto` tiene `porcentaje_base_gravada` (default 100) para regimenes PY de base reducida (ej. gravado solo el 20% o 30% del precio). El motor de documento calcula gravadas/IVA sobre `monto * base/100` y el resto va a exentas.
2. **ABM de impuestos con modo simplificado/avanzado**: el parametro `IMPUESTOS_MODO_AVANZADO` (parametro_sistema, default 'NO') controla la pantalla:
   - `NO` (simplificado): el campo base gravada NO se muestra (queda fijo en 100); el administrador ve solo descripcion + porcentaje — sin complicaciones.
   - `SI` (avanzado): se muestra y edita `porcentaje_base_gravada`, con los factores calculados automaticamente a partir del porcentaje.
   El objetivo textual del usuario: "mostrar solo impuestos mas simples y no complicar a los administradores".

## Objetivo Funcional

[Que debe poder hacer el usuario o el sistema]

## Criterios De Aceptacion

- [ ] Criterio 1
- [ ] Criterio 2
- [ ] Criterio 3

## Dependencias

- Depende de: ninguna
- Requerido por: ninguno
