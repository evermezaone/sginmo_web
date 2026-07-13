# REQ-0080 - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-13
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0080
- Tipo de cambio: backend (ComprobanteService). Sin BD, sin UI.
- Riesgo: bajo (fix de cast defensivo).
- Archivos clave:
  - `servicio/ComprobanteService.java`: helper `aLocalDate(Object)` que acepta java.time.LocalDate, java.sql.Date o Timestamp; se usa en `cobrosRecientes()` (linea 66) y `reciboCobro()` (linea 95), reemplazando el cast directo `(java.sql.Date)` que rompia con Hibernate 7.
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - Deploy VPS; `python tools/smoke-test-vps.py`: 36/36 (comprobantes 200; antes 500).
- Cambios de datos: no. Cambios de entorno: no.
- Decision esperada: aprobar; es un fix defensivo de conversion de fecha.
- Notas para auditor:
  - Bug latente: solo se disparaba con al menos un cobro en la ventana. Detectado por el smoke-test al registrar un cobro real (REQ-0079).
  - El mismo patron defensivo ya se usa en otros servicios (CajaBean.getCobroAnulableId, DrilldownService.fecha).

## Resumen Funcional

La pantalla de comprobantes vuelve a abrir y listar cobros sin error 500.

## Resumen Tecnico

Conversion de fecha tolerante al tipo que devuelve el driver (LocalDate/sql.Date/Timestamp) en ComprobanteService.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| servicio/ComprobanteService.java | helper aLocalDate() + uso en cobrosRecientes y reciboCobro |

## Cambios De Datos

Sin cambios.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; deploy VPS; smoke 36/36 (comprobantes 200; reproducido el 500 previo y verificado el fix).

## Pruebas Manuales Sugeridas

1. Con al menos un cobro registrado, abrir comprobantes -> lista sin error.
2. Reimprimir un recibo (reciboCobro) -> genera el PDF con la fecha correcta.

## Limitaciones Conocidas

- Ninguna. Puede haber otros servicios con el mismo cast directo (no detectados por el smoke por falta de datos); se corrigen si aparecen.

## Riesgos Conocidos

- Ninguno relevante (fix de conversion).
