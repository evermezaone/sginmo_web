# REQ-0015 - Lotes y generacion masiva de lotes

**Estado:** implementado (2026-07-06)

## Objetivo Funcional
Generacion masiva de lotes: crear N lotes hijos de un loteamiento contenedor, numerados correlativamente desde un numero base, con manzana, precio y comision comunes.

## Criterios De Aceptacion
- [x] Accion 'Generar lotes' en el ABM de activos (dialogo con contenedor, manzana, desde, cantidad max 500, precio, comision).
- [x] ActivoService.generarLotes crea los lotes en UNA transaccion, omite numeros ya existentes bajo ese padre, hereda empresa/ubicacion del contenedor.
- [x] Enforcement de permiso CREAR en el servicio.
- [x] Desplegado y verificado (HTTP 200, boton presente).

## Bloqueo Formal Documentado
Validacion visual del usuario PENDIENTE (estrategia desarrollo continuo 0012-0032).
