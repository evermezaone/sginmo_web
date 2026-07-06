# Implementacion Claude - REQ-0029

## Manifiesto Minimo Para Codex
Recaudacion de la planilla de caja en PDF (cobros del periodo, apertura+cobrado=total). Los egresos se listan en la pantalla de ingresos-egresos con filtro por tipo.

**Archivos:** ReporteService.recaudacionPlanilla (ver REQ-0026); pantalla ingresos-egresos (REQ-0024).

**Comandos probados:** mvn clean package EXIT 0; deploy; verificacion HTTP/PDF contra la VPS.
