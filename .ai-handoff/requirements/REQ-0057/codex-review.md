# REQ-0057 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-12
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos Bloqueantes

- `MoraService.registrarGestion` y `registrarPromesa` asignan el tenant actual, pero aceptan `operacion`, `cronograma_cuota` y `cliente` desde el bean sin revalidar que pertenezcan al tenant actual y sean coherentes entre si. Las FK solo validan existencia global; no impiden asociar una gestion/promesa del tenant actual a una operacion/cuota/persona de otro tenant si se manipula el postback.

## Solucion Esperada

- Antes de persistir, cargar/validar la operacion y cuota bajo `@AislarTenant`.
- Verificar que la cuota pertenece a la operacion y que el cliente corresponde a la operacion.
- Rechazar ids inexistentes, cruzados o fuera del tenant con mensaje de negocio.

## Pruebas Revisadas

- Revision estatica de `MoraService`, `MoraBean`, `V38__mora_cobranza.sql` y la integracion en `AgendaService`.
- Build Maven previo: `mvn -q clean package` EXIT 0.
