# Implementacion Claude - REQ-0018

## Manifiesto Minimo Para Codex
Al crear la operacion se generan automaticamente documentos internos: deposito de garantia (alquiler) y comision segun el % del activo (comision_alquiler/comision_venta). Verificado en E2E (comision 5% de 10M = 500.000).

**Archivos:** OperacionService.crear (crearDocumentoInterno para garantia y comision).

**Arquitectura:** BD-centrica (doc 10): toda la matematica financiera vive en V16/V17 como triggers y funciones PL/pgSQL; los services Java las INVOCAN (SELECT f_...), jamas recalculan cuadres.

**Comandos probados:** mvn -q clean package (multi-modulo EXIT 0); deploy-vps.ps1; bateria SQL de 9 checks + E2E (operacion 10 cuotas -> cobro 2M cancela 2 cuotas + caja -> anular repone) todos OK en la VPS.
