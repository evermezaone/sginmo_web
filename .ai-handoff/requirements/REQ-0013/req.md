# REQ-0013 - Entidades inmobiliarias y propietarios

**Estado:** implementado (2026-07-06), pendiente validacion visual del usuario

## Objetivo Funcional
Cubierto por la tabla `activo` recursiva (reemplaza entidades_inmobiliarias del legado, decision de diseno). ABM de activos con contenedor (padre) por autocomplete, propietarios (activo_propietario, personas con rol PROPIETARIO), y atributos parametrizables por tipo (obligatoriedad validada). V15 pantalla.

## Criterios De Aceptacion
- [x] ABM con patron estandar completo.
- [x] Enforcement de permisos en la capa de servicio.
- [x] Desplegado y verificado por HTML en la VPS (HTTP 200, sin errores).
- [ ] Validacion visual/funcional del usuario: PENDIENTE.
