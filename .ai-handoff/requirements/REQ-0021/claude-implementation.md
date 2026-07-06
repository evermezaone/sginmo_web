# Implementacion Claude - REQ-0021

## Manifiesto Minimo Para Codex
Finalizar/rescindir una operacion vigente: estado FINALIZADO, el activo vuelve a LIBRE (salvo venta consumada) e inserta la rescision con motivo. Pestana Finalizar/Rescindir del detalle.

**Archivos:** OperacionService.finalizar (+ insert en rescision), operaciones.xhtml.

**Arquitectura:** BD-centrica (doc 10): toda la matematica financiera vive en V16/V17 como triggers y funciones PL/pgSQL; los services Java las INVOCAN (SELECT f_...), jamas recalculan cuadres.

**Comandos probados:** mvn -q clean package (multi-modulo EXIT 0); deploy-vps.ps1; bateria SQL de 9 checks + E2E (operacion 10 cuotas -> cobro 2M cancela 2 cuotas + caja -> anular repone) todos OK en la VPS.
