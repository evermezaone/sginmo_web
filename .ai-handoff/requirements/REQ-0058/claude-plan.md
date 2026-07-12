# REQ-0058 - Plan De Implementacion

**Estado:** APROBADO_PARA_DESARROLLO
**Fecha:** 2026-07-12

## Estrategia

Reutilizar PdfService (OpenPDF, REQ-0026) en un ComprobanteService que arme el recibo de cobro desde el
cobro persistido. Pantalla de comprobantes para listar/descargar/reimprimir. Sin Jasper.

## Archivos A Modificar

| Archivo | Cambio |
|---|---|
| V39__pantalla_comprobantes.sql | registra pantalla |
| servicio/ComprobanteService.java | NUEVO — recibo de cobro OpenPDF |
| web/ComprobanteBean.java + webapp/comprobantes.xhtml | NUEVOS |
| WEB-INF/plantilla.xhtml + smoke | menu + cobertura |

## Pruebas Previstas

- [ ] Build OK
- [ ] V39 rollback + deploy + smoke
- [ ] Sin dependencia Jasper; reimpresion trazable

## Riesgos

- Bajo-medio: solo lectura. Generar solo desde transaccion persistida.

## Cambios De Datos

V39 registra pantalla `comprobantes`.
