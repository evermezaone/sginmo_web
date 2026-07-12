# REQ-0046 - BUG: alta de operacion falla por moneda NOT NULL

**Numero:** REQ-0046
**Fecha de creacion:** 2026-07-11
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

BUG: el alta de operacion falla con error de integridad porque la columna `moneda` es NOT NULL
y el formulario de alta no tiene selector de moneda. No se puede registrar ninguna operacion.

## Objetivo Funcional

Agregar un selector de Moneda obligatorio en el formulario de alta/edicion de operacion, con
un valor por defecto razonable (Guaranies) para que el registro de la operacion no falle por
restriccion de integridad `moneda null`.

## Criterios De Aceptacion

- [x] El formulario de alta de operacion tiene selector de Moneda obligatorio.
- [x] Se puede registrar una operacion sin error de integridad por moneda null.
- [x] La moneda por defecto es Guaranies cuando existe.

## Reglas De Negocio

- La moneda es obligatoria para toda operacion (columna NOT NULL en la BD).
- Solo se ofrecen monedas ACTIVAS visibles al tenant (globales -1 + propias).

## Dependencias

- Depende de: catalogo de Moneda existente (sin migracion nueva).
- Requerido por: registro de operaciones (flujo bloqueado sin este fix).

## Fuentes Y Trazabilidad

- Reporte de bug del usuario: alta de operacion falla con "null value in column moneda".
