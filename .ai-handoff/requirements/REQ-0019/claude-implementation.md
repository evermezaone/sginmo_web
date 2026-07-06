# Implementacion Claude - REQ-0019

## Manifiesto Minimo Para Codex
Regenerar el cronograma con otra cantidad/fecha SOLO si la operacion aun no tiene cobros; la BD lo garantiza (f_generar_cronograma lanza excepcion si hay cuotas con cobros). Pestana Regenerar del detalle.

**Archivos:** OperacionService.regenerarCuotas, f_generar_cronograma (guarda), operaciones.xhtml.

**Arquitectura:** BD-centrica (doc 10): toda la matematica financiera vive en V16/V17 como triggers y funciones PL/pgSQL; los services Java las INVOCAN (SELECT f_...), jamas recalculan cuadres.

**Comandos probados:** mvn -q clean package (multi-modulo EXIT 0); deploy-vps.ps1; bateria SQL de 9 checks + E2E (operacion 10 cuotas -> cobro 2M cancela 2 cuotas + caja -> anular repone) todos OK en la VPS.
