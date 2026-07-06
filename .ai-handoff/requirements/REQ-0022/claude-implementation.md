# Implementacion Claude - REQ-0022

## Manifiesto Minimo Para Codex
Caja diaria (planilla por sucursal) + cobros que invocan f_cobrar_documento (V17): baja el saldo del documento via trigger, cancela cuotas FIFO por vencimiento y suma a la caja. La mora se calcula en la BD (f_mora_cuota = dias_atraso - gracia, * mora diaria) y se muestra por cuota. Verificado numericamente.

**Archivos:** Planilla (entidad), CajaService (abrir/cerrar/cobrar), CajaBean, caja.xhtml, V17 f_cobrar_documento/f_mora_cuota, V18.

**Arquitectura:** BD-centrica (doc 10): toda la matematica financiera vive en V16/V17 como triggers y funciones PL/pgSQL; los services Java las INVOCAN (SELECT f_...), jamas recalculan cuadres.

**Comandos probados:** mvn -q clean package (multi-modulo EXIT 0); deploy-vps.ps1; bateria SQL de 9 checks + E2E (operacion 10 cuotas -> cobro 2M cancela 2 cuotas + caja -> anular repone) todos OK en la VPS.
