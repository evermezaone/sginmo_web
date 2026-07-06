# Implementacion Claude - REQ-0023

## Manifiesto Minimo Para Codex
Anulacion de cobro que invoca f_anular_cobro (V17): repone el saldo del documento, reabre las cuotas afectadas y descuenta de la caja, todo en la BD. Boton anular en la lista de cobros de la planilla. Verificado (repone 10M/caja 0).

**Archivos:** CajaService.anularCobro, f_anular_cobro (V17), caja.xhtml.

**Arquitectura:** BD-centrica (doc 10): toda la matematica financiera vive en V16/V17 como triggers y funciones PL/pgSQL; los services Java las INVOCAN (SELECT f_...), jamas recalculan cuadres.

**Comandos probados:** mvn -q clean package (multi-modulo EXIT 0); deploy-vps.ps1; bateria SQL de 9 checks + E2E (operacion 10 cuotas -> cobro 2M cancela 2 cuotas + caja -> anular repone) todos OK en la VPS.
