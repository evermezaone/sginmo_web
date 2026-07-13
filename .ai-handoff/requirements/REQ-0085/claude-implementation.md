# REQ-0085 (Fase 3) - Implementacion

**Estado:** LISTO_PARA_AUDITORIA_CODEX
**Fecha:** 2026-07-13
**Rama:** multiempresa

## Manifiesto Minimo Para Codex

- REQ: REQ-0085 (Fase 3, conciliacion bancaria; depende de 0083 Fase 1 y 0084 Fase 2).
- Tipo de cambio: BD (V58) + servicio (conciliacion) + bandeja (candidatos + carga/import de movimientos).
- Riesgo: medio-alto (aplica cobros con match bancario).
- Fuente confirmada: carga manual + importacion CSV (sin credenciales del banco; IMAP como extension).
- Archivos clave:
  - `V58__movimiento_bancario.sql`: `movimiento_bancario_importado` (fuente, banco, cuenta, fecha, importe, moneda,
    referencia, remitente, estado_conciliacion, hash_externo, transferencia) con RLS por tenant; unique parcial por
    (tenant, hash_externo) = idempotencia de importacion; params PORTAL_TRANSF_TOLERANCIA_DIAS / MONTO_MAX_AUTO.
  - `servicio/PortalTransferenciaService.java`: registrarMovimiento (manual), importarCsv (banco;cuenta;fecha;importe;
    referencia;remitente, dedup por hash), movimientos(estado), candidatos(transferencia) (mismo importe + fecha con
    tolerancia + referencia), conciliarYAplicar (marca el movimiento CONCILIADO + aplica via aprobar()/motor de caja).
  - `web/TransferenciaBandejaBean.java`: candidatos al seleccionar, conciliar(mov), registrarMovimiento, importarCsv.
  - `webapp/transferencias.xhtml`: en el dialogo de revision, tabla de "Movimientos bancarios candidatos" con
    "Conciliar" (aplica) + "Aprobar sin conciliar" (fallback manual); dialogo "Movimientos bancarios" para carga
    manual e importacion CSV.
- Comandos probados:
  - `mvn -q clean package`: BUILD OK.
  - Deploy VPS + Flyway V58 (schema v58); `python tools/smoke-test-vps.py`: 37/37 (transferencias 200).
- Cambios de datos: si, V58 (tabla + RLS + params). Cambios de entorno: no.
- Decision esperada: aprobar Fase 3; cierra la plataforma de transferencia (0083/0084/0085).
- Notas para auditor:
  - No aplica sin match confirmado: el operador concilia con un movimiento (o usa el fallback "Aprobar sin conciliar"
    explicito). La aplicacion pasa por el motor de caja (requiere caja abierta) -> no duplica reglas.
  - Anti-doble: el movimiento queda CONCILIADO (no reutilizable); numero_transaccion unique parcial sobre APLICADO (V56).
  - Idempotencia de importacion por hash_externo (unique parcial).
  - Autoaplicacion desatendida no es viable sin caja abierta; es asistida por el operador (documentado).

## Resumen Funcional

El operador carga/importa los avisos del banco y, en la bandeja, ve los movimientos que coinciden con cada
transferencia; al conciliar con uno, se aplica el pago. Ningun movimiento se usa dos veces.

## Resumen Tecnico

Tabla de movimientos con RLS + idempotencia; cruce por importe/fecha/referencia; conciliacion que reutiliza el aprobar de Fase 1.

## Archivos Modificados

| Archivo | Cambio |
|---|---|
| V58__movimiento_bancario.sql | NUEVO - tabla + RLS + params |
| servicio/PortalTransferenciaService.java | registrar/importar/movimientos/candidatos/conciliarYAplicar + DTO Mov |
| web/TransferenciaBandejaBean.java | candidatos + conciliar + registrar/importar |
| webapp/transferencias.xhtml | candidatos en el dialogo + dialogo de carga/importacion |

## Cambios De Datos

V58: movimiento_bancario_importado (RLS) + params PORTAL_TRANSF_TOLERANCIA_DIAS / MONTO_MAX_AUTO.

## Variables De Entorno

Sin cambios.

## Pruebas Ejecutadas

Build OK; deploy VPS; Flyway V58; smoke 37/37.

## Pruebas Manuales Sugeridas

1. Movimientos bancarios -> registrar/importar CSV.
2. En una transferencia RECIBIDO con mismo importe/fecha -> aparece como candidato -> Conciliar -> aplica el cobro.
3. Intentar conciliar el mismo movimiento otra vez -> rechaza (ya CONCILIADO).

## Limitaciones Conocidas

- Autoaplicacion asistida por operador (no desatendida) por la dependencia de caja abierta. IMAP/API como extension.

## Riesgos Conocidos

- Maneja dinero: mitigado con match confirmado, motor de caja, anti-doble (movimiento + nro transaccion), RLS y auditoria.
