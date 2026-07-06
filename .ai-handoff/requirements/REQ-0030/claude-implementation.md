# Implementacion Claude - REQ-0030

## Manifiesto Minimo Para Codex
El index es un tablero con 7 indicadores del negocio: activos LIBRES/OCUPADOS/VENDIDOS, operaciones vigentes, cuotas vencidas (en rojo), recaudado hoy y saldo por cobrar (este ultimo de la vista v_operacion_saldo del motor). Debajo, las tarjetas de acceso por permiso.

**Archivos:** InicioBean (consultas de agregacion + v_operacion_saldo), index.xhtml (fila .kpis).

**Comandos probados:** mvn clean package EXIT 0; deploy; verificacion contra la VPS.
