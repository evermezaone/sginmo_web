# REQ-0087 - Auditoria Codex

**Estado:** APROBADO_POR_CODEX
**Fecha:** 2026-07-13
**Auditor:** Codex

## Decision

**APROBADO_POR_CODEX**

## Revision Estatica

- `V55__activo_lotes_casas.sql` agrega las columnas solicitadas en `activo` como opcionales y mantiene constraint para `cochera`.
- `Activo.java` mapea los campos nuevos (`superficie`, `dimensionesLinderos`, `cochera`, `m2Construccion`, `medida`, ANDE y ESSAP) con getters/setters.
- `ActivoBean` implementa `generarTipo`, modos de formulario, filtro de tipos para casas/dptos, lista de cocheras y boton unico `guardarDialogo()`.
- `activos.xhtml` muestra `GENERAR` solo en alta, renderiza generacion masiva para LOTES, oculta los required del formulario comun en ese modo y muestra secciones condicionales para lote o casa/dpto.
- `ActivoService.guardar()` sigue completando tenant en altas y preservando tenant en edicion; `generarLotes()` conserva validaciones de contenedor, cantidad, tenant y duplicados.

## Hallazgos

### Bloqueantes

- Ninguno.

### No Bloqueantes

- El combo embebido de generacion masiva lista `loteamientos()` y no todos los tipos que el service acepta como contenedores (`BARRIO_CERRADO` tambien es valido por regla de service). No bloquea porque el REQ habla de loteamiento y la validacion server-side sigue correcta.
- Los adjuntos inline y combos dedicados para loteamiento/complejo quedan correctamente documentados como follow-up fuera de esta entrega.

## Riesgos

- Reestructura de UI con render condicional; conviene prueba manual real de alta LOTES, alta CASAS/DPTOS y edicion de lote, aunque la compilacion y la revision estatica no muestran fallas.

## Pruebas Revisadas

- [x] Revision estatica de migracion V55.
- [x] Revision estatica de entidad `Activo`.
- [x] Revision estatica de `ActivoBean`.
- [x] Revision estatica de `activos.xhtml`.
- [x] Revision de persistencia en `ActivoService`.
- [x] `mvn -q -pl sginmo-web -am clean package` EXIT 0.

## Pruebas Faltantes

- [ ] Prueba manual desde navegador: Nuevo -> LOTES -> Generar.
- [ ] Prueba manual desde navegador: Nuevo -> CASAS/DPTOS -> Guardar -> reeditar.
- [ ] Prueba manual desde navegador: editar lote existente -> guardar superficie/dimensiones -> reeditar.
