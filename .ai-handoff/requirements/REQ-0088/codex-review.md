# REQ-0088 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-13
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Revision Estatica

- `V54__activo_campos_operacion.sql` agrega `tipo_operacion`, `medidas`, `anio` y `cantidad_unidades` como columnas opcionales.
- `tipo_operacion` tiene CHECK para `ALQUILER`/`VENTA` y admite `NULL`, por lo que no rompe datos existentes.
- `Activo.java` mapea los cuatro campos con getters/setters, quedando cubiertos por el `persist/merge` existente.
- `activos.xhtml` muestra Operacion, Medidas, Anio y Cantidad de unidades en el formulario de activo individual.
- La etiqueta del autocompletado de padre fue renombrada a `Caracteristicas`.

## Hallazgos

### Bloqueantes

- Ninguno.

### No Bloqueantes

- En el flujo nuevo de `REQ-0087`, estos campos no aparecen cuando `GENERAR=LOTES` porque ese modo ejecuta generacion masiva, no alta individual. Para altas/ediciones individuales se muestran correctamente.

## Riesgos

- Bajo: son campos opcionales y la persistencia reutiliza el service existente.

## Pruebas Revisadas

- [x] Revision estatica de migracion V54.
- [x] Revision estatica de `Activo.java`.
- [x] Revision estatica de `activos.xhtml`.
- [x] `mvn -q -pl sginmo-web -am clean package` EXIT 0.

## Pruebas Faltantes

- [ ] Prueba manual desde navegador: guardar activo con los nuevos campos y reeditar.
