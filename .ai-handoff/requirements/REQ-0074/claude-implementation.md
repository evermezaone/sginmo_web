# REQ-0074 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-12
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0074
- Tipo de cambio: backend (DrilldownService) + UI (dashboard-detalle.xhtml/Bean). Sin BD.
- Riesgo: medio (evidencia de datos sensibles; anti-injection y permisos por modulo)
- Archivos clave:
  - `servicio/DrilldownService.java` (@AislarTenant): whitelist clave->permiso; detalle(clave, desde, hasta, moneda, sucursal, ref) con consultas PARAMETRIZADAS por clave (cobros/mora/ingresos/egresos/ocupacion/vacancia/rentabilidad_activo). Exige el permiso del modulo origen ademas de dashboard-gerencial/VER.
  - `web/DashboardDetalleBean.java` + `webapp/dashboard-detalle.xhtml`: view params tipados; tabla dinamica; export CSV con filtros + fecha de generacion; estado vacio si no hay clave.
  - `tools/smoke-test-vps.py`: cobertura.
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - Deploy + `python tools/smoke-test-vps.py`: 35/35 incl. `dashboard-detalle` (tras cambiar p:message sin `for` por div).
- Cambios de datos: no. Cambios de entorno: no.
- Decision esperada: aprobar; revisar la whitelist anti-injection y los permisos por modulo.
- Notas para auditor:
  - Anti-injection: NO se arma SQL con entrada libre; solo claves de una whitelist fija, cada una con su query parametrizada; filtros tipados (LocalDate/Long).
  - Permisos: cada clave exige (pantalla,accion) del modulo origen; ver dashboard no basta.
  - No mezcla monedas: el detalle de cobros/mora filtra por moneda cuando se pasa.
  - RLS: @AislarTenant; contexto global -> vacio. Cada detalle muestra filtros + fecha/hora de generacion.

## Resumen Funcional

Desde el dashboard, cada KPI puede abrir "la evidencia": la lista exacta de registros que componen el
numero (cobros, cuotas en mora, movimientos, propiedades ocupadas/vacantes, movimientos de un activo),
con los filtros aplicados y exportable a CSV.

## Resumen Tecnico

DrilldownService con whitelist + consultas parametrizadas; pantalla generica con tabla dinamica y CSV.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| servicio/DrilldownService.java | NUEVO |
| web/DashboardDetalleBean.java + webapp/dashboard-detalle.xhtml | NUEVOS |
| tools/smoke-test-vps.py | cobertura |

## Cambios De Datos

Sin cambios.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; deploy; smoke 35/35 (dashboard-detalle 200 tras fix del p:message).

## Pruebas Manuales Sugeridas

1. Abrir dashboard-detalle.xhtml?clave=mora&hasta=YYYY-MM-DD&moneda=ID -> lista de cuotas vencidas.
2. clave=vacancia -> propiedades vacantes; exportar CSV.
3. Con un usuario sin permiso del modulo origen -> el detalle no se muestra.

## Riesgos Conocidos

- Evidencia sensible: mitigado (whitelist + permiso por modulo + RLS). PDF de detalle: CSV por ahora (PDF incremental).
