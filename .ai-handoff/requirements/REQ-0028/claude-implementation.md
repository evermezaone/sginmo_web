# Implementacion Claude - REQ-0028

## Manifiesto Minimo Para Codex
Recibo de cobro (comprobantes aplicados + total) y Estado de cuenta de operacion (cronograma con mora del dia, usando v_operacion_saldo) en PDF estandar. Botones en caja y operaciones.

**Archivos:** ReporteService.reciboCobro/estadoCuenta, DescargaBean (ver REQ-0026).

**Comandos probados:** mvn clean package EXIT 0; deploy; verificacion HTTP/PDF contra la VPS.
