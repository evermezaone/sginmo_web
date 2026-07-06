# Implementacion Claude - REQ-0020

## Manifiesto Minimo Para Codex
Extiende el contrato N meses agregando cuotas nuevas al cronograma (con nuevo precio opcional), actualiza fecha_fin_contrato, fecha_renovacion, monto_total y plazo. Pestana Renovar del detalle.

**Archivos:** OperacionService.renovar, operaciones.xhtml.

**Arquitectura:** BD-centrica (doc 10): toda la matematica financiera vive en V16/V17 como triggers y funciones PL/pgSQL; los services Java las INVOCAN (SELECT f_...), jamas recalculan cuadres.

**Comandos probados:** mvn -q clean package (multi-modulo EXIT 0); deploy-vps.ps1; bateria SQL de 9 checks + E2E (operacion 10 cuotas -> cobro 2M cancela 2 cuotas + caja -> anular repone) todos OK en la VPS.
