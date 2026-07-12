# REQ-0062 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | Build OK | OK |
| T02 | V43 en `BEGIN...ROLLBACK` | pantalla registrada | OK |
| T03 | Deploy + Flyway V43 | success=t | OK |
| T04 | `python tools/smoke-test-vps.py` | 28/28 render OK incl. reportes | OK (TODAS OK) |
| T05 | Sin Jasper | `grep -ri jasper */pom.xml` sin resultados; sin .jrxml | OK |

## Pruebas Manuales

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | Cobros por periodo -> PDF | tabla con fecha/cliente/forma/moneda/monto + total | pendiente |
| M02 | Cobros por periodo -> CSV | UTF-8, coma, campos entrecomillados si hace falta | pendiente |
| M03 | Mora -> PDF/CSV | cartera vencida con dias | pendiente |
| M04 | Propiedades -> PDF/CSV | activos LIBRE | pendiente |

## Revision Transversal

- Sin Jasper: reutiliza PdfService (OpenPDF); grep jasper en poms sin resultados.
- "No mezclar monedas": cobros filtra por moneda; montos con BigDecimal.
- "Coincide con la fuente transaccional": cobros de la tabla cobro (estado ACTIVO), no de UI.
- Aislamiento: @AislarTenant -> RLS por tenant; permiso reportes VER/EXPORTAR.
- Limite: EXPORT_LIMITE_FILAS (REQ-0060) aplicado en setMaxResults (no agota memoria).

## Datos De Prueba

Empresa con activos, cobros y cuotas. Render verificado en prod (smoke).
