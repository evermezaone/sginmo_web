# REQ-0054 - Plan De Pruebas

**Fecha:** 2026-07-12

## Pruebas Tecnicas

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| T01 | `mvn -q clean package` | Build OK | OK |
| T02 | V35 en `BEGIN...ROLLBACK` | 4 columnas nuevas + pantalla | OK (columnas=4, pantalla=1) |
| T03 | Backup previo | dump OK | OK |
| T04 | Deploy + Flyway V35 | success=t | OK |
| T05 | `python tools/smoke-test-vps.py` | 22/22 render OK incl. documentos-generados | OK (TODAS OK) |

## Pruebas Manuales

| ID | Escenario | Resultado esperado | Resultado real |
|---|---|---|---|
| M01 | Gestionar un documento generado: Impreso/Enviado/Firmado | estado + fecha sellados | pendiente (requiere documentos generados de REQ-0041) |
| M02 | Anular con motivo | estado ANULADO + motivo/usuario/fecha; archivo conservado | pendiente |
| M03 | Filtro "solo pendientes de firma" | oculta FIRMADO/ANULADO/ARCHIVADO | pendiente |

## Revision Transversal

- Se extiende una tabla que YA tiene RLS (V29); no se agregan politicas (correcto). El servicio es
  @AislarTenant y usa `em.find` bajo RLS -> solo documentos del tenant.
- Regla "anular no borra archivo/historial": anular() solo cambia estado + registra motivo/usuario/fecha.
- Regla "documento firmado no se regenera silenciosamente": este REQ no toca la generacion (REQ-0041);
  el estado FIRMADO es el control. La proteccion en regeneracion queda para el flujo de REQ-0041 (notado).
- Permisos: `exigir("documentos-generados","EDITAR")` para estado/firma; `INACTIVAR` para anular.

## Datos De Prueba

Requiere documentos generados por REQ-0041 (contratos/pagares) en una empresa real.
