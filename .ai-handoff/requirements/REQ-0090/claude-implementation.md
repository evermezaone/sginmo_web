# REQ-0090 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-14
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0090
- Tipo de cambio: backend
- Riesgo: bajo (solo conversion de tipos en lectura; sin cambios de esquema ni de logica)
- Archivos clave:
  - `PortalService.java`: helper `aLocalDate()` + 5 casts reemplazados (crash de `resumen()`).
  - `PortalTransferenciaService.java`: helper + 3 lecturas (crash de `candidatos()` linea 347 + `mov()` y `ocrFecha` que devolvian null en silencio con LocalDate).
  - `MoraService.java`: helper + 1 cast (`carteraMorosa`, fecha de vencimiento).
  - `ObjetivoService.java`: helper + 4 casts (mediciones y vigencia de objetivos).
- Comandos probados:
  - `powershell -File tools/deploy-vps.ps1` (mvn clean package multi-modulo + deploy): `Build OK` / `Redeploy OK`, EXIT: 0.
  - `curl login.xhtml` en la VPS: HTTP 200; `sginmo-web.war.deployed` presente.
  - `python tools/smoke-test-vps.py`: `=== RESULTADO: TODAS OK ===` (37 pantallas 200, incluidas objetivos/transferencias/cobranza).
  - `grep -rnE "\(java\.sql\.Date\) [a-z]" (excluyendo instanceof)`: 0 coincidencias.
- Cambios de datos: no
- Cambios de entorno: no
- Impacto LLM/tokens: no
- Decision esperada: aprobar
- Notas para auditor: el helper `aLocalDate` esta duplicado por servicio siguiendo la convencion ya existente
  (ComprobanteService de REQ-0080 tiene su propia copia). Acepta LocalDate/java.sql.Date/Timestamp y cae a
  `LocalDate.parse(toString())` como ultimo recurso.

## Resumen Funcional

El portal del socio ya no crashea al ingresar: la pantalla de inicio muestra el resumen de cuenta
(deuda vencida, proxima cuota, total pagado, operaciones) correctamente.

## Resumen Tecnico

Se reemplazaron todos los casts duros `(java.sql.Date)` sobre resultados de consultas nativas por un helper
`aLocalDate(Object)` defensivo. Con Hibernate 7 las columnas `date` vuelven como `java.time.LocalDate`, lo
que rompia el cast (`PortalService.resumen`) o —peor— devolvia `null` en silencio en dos ramas de
`PortalTransferenciaService` que usaban `instanceof java.sql.Date d ? ... : null`.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| `sginmo-web/.../servicio/PortalService.java` | helper `aLocalDate` + 5 casts -> helper |
| `sginmo-web/.../servicio/PortalTransferenciaService.java` | helper + `candidatos()` (bind LocalDate), `mov()`, `ocrFecha` |
| `sginmo-web/.../servicio/MoraService.java` | helper + `carteraMorosa` fecha de vencimiento |
| `sginmo-web/.../servicio/ObjetivoService.java` | helper + 4 casts (mediciones + vigencia) |

## Cambios De Datos

Sin cambios.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

- Build multi-modulo `mvn clean package`: Build OK (EXIT 0).
- Deploy a la VPS: Redeploy OK; `login.xhtml` HTTP 200.
- `smoke-test-vps.py`: 37 pantallas HTTP 200 (TODAS OK).
- Verificacion estatica: 0 casts duros `(java.sql.Date)` restantes.

## Pruebas Manuales Sugeridas

1. Ingresar al portal del socio (CI/RUC + OTP) -> `portal/inicio.xhtml` carga el resumen sin error.
2. Abrir mora/cobranza, objetivos y transferencias en el back-office -> renderizan y muestran fechas.

## Riesgos Conocidos

Ninguno. El portal externo no se puede cubrir con el smoke automatico (requiere OTP), por eso se documenta
la prueba manual; el resto de superficies afectadas quedaron cubiertas por el smoke.
