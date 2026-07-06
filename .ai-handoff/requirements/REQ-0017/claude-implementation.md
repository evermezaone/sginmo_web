# Implementacion Claude - REQ-0017

## Manifiesto Minimo Para Codex
Generacion del cronograma en la BD (f_generar_cronograma, V16): reparte el total en N cuotas mensuales con dia_pago fijo; la ultima cuota absorbe el redondeo -> la suma cuadra EXACTA al total (verificado). Visible en la pestana Cronograma del detalle de operacion.

**Archivos:** V16 f_generar_cronograma, CronogramaCuota (entidad), OperacionService, operaciones.xhtml.

**Arquitectura:** BD-centrica (doc 10): toda la matematica financiera vive en V16/V17 como triggers y funciones PL/pgSQL; los services Java las INVOCAN (SELECT f_...), jamas recalculan cuadres.

**Comandos probados:** mvn -q clean package (multi-modulo EXIT 0); deploy-vps.ps1; bateria SQL de 9 checks + E2E (operacion 10 cuotas -> cobro 2M cancela 2 cuotas + caja -> anular repone) todos OK en la VPS.
