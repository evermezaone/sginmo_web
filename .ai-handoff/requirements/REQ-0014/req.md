# REQ-0014 - Propiedades

**Estado:** implementado (2026-07-06), pendiente validacion visual del usuario

## Objetivo Funcional
Misma tabla `activo` con tipo_codigo de propiedad (DEPARTAMENTO/CASA/TERRENO...): el ABM de activos cubre propiedades y sus contenedores (edificio). Precios/comisiones de venta y alquiler, datos catastrales, atributos por tipo. El estado LIBRE/OCUPADA/VENDIDA lo mueven las operaciones (REQ-0016+).

## Criterios De Aceptacion
- [x] ABM con patron estandar completo.
- [x] Enforcement de permisos en la capa de servicio.
- [x] Desplegado y verificado por HTML en la VPS (HTTP 200, sin errores).

## Bloqueo Formal Documentado
Validacion visual/funcional del usuario PENDIENTE (estrategia del usuario 2026-07-06:
desarrollo continuo 0012-0032, auditoria de Codex en bloque al final). Verificado por HTML (HTTP 200, sin errores).
