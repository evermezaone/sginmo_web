# REQ-0027 - Auditoria Codex

**Estado:** REQUIERE_CAMBIOS
**Fecha:** 2026-07-08
**Auditor:** Codex

## Decision

**REQUIERE_CAMBIOS**

## Hallazgos

### Bloqueantes

- Obs 235: el PDF de activos/propiedades no tiene enforcement de permiso `EXPORTAR`. El boton en `activos.xhtml` no tiene `rendered="#{sesionUsuario.puede('activos','EXPORTAR')}"`, y `DescargaBean.listadoActivos()` / `ReporteService.listadoActivos()` tampoco exigen permiso en backend. El REQ pide enforcement de permisos y el estandar ya trata exportacion como permiso separado (por ejemplo `articulos.xhtml`). Impacto: un usuario con solo `VER` puede exportar el listado completo de propiedades.

### No Bloqueantes

- El reporte usa OpenPDF via `PdfService`, no Jasper.
- El boton usa `ajax=false`, adecuado para descarga.
- La consulta incluye nombre, tipo, precios y situacion como pide el REQ.

## Riesgos

- Exfiltracion de datos de propiedades por usuarios sin permiso de exportacion.

## Pruebas Revisadas

- [x] Revision estatica de `ReporteService.listadoActivos`.
- [x] Revision estatica de `DescargaBean.listadoActivos`.
- [x] Revision estatica de `activos.xhtml`.
- [x] Comparacion con estandar de permisos de exportacion en ABM.

## Pruebas Faltantes

- [ ] Reejecutar `mvn -q clean package` luego de corregir.
- [ ] Prueba funcional: usuario con `VER` sin `EXPORTAR` no debe ver/ejecutar el PDF; usuario con `EXPORTAR` si.
