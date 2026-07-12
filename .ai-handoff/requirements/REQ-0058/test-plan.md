# REQ-0058 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | Build OK | OK |
| T02 | V39 en `BEGIN...ROLLBACK` | pantalla registrada | OK |
| T03 | Backup previo | dump OK | OK |
| T04 | Deploy + Flyway V39 | success=t | OK |
| T05 | `python tools/smoke-test-vps.py` | 25/25 render OK incl. comprobantes | OK (TODAS OK) |
| T06 | Sin dependencia Jasper | pom sin jasperreports/.jrxml | OK (reutiliza PdfService/OpenPDF) |

## Pruebas Manuales

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | Descargar recibo de un cobro | PDF con cliente/forma de pago/moneda/monto/detalle/usuario | pendiente (requiere cobros registrados) |
| M02 | Reimprimir el mismo recibo | mismo contenido; encabezado con numero/empresa/usuario/fecha-hora | pendiente |

## Revision Transversal

- Sin Jasper: se reutiliza `PdfService` (OpenPDF); `grep -ri jasper Desarrollo/**/pom.xml` sin resultados.
- Generar solo desde transaccion persistida: `reciboCobro` lee el cobro por id (RLS por tenant); si no existe, error.
- Formato de dinero: DecimalFormat es-PY (mismo criterio que el resto de PDFs/UI).
- Reimpresion: no se persiste el PDF; se regenera desde el cobro inmutable (trazabilidad por numero/fecha/usuario).

## Datos De Prueba

Empresa con cobros registrados (modulo de caja/cobros, REQ-0022).
