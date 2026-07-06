# Implementacion Claude - REQ-0016

## Manifiesto Minimo Para Codex
Alta de operacion de alquiler/venta: valida activo LIBRE, calcula monto total (precio*plazo en alquiler credito), crea el documento cta cte y (a credito) genera el cronograma invocando f_generar_cronograma; el activo pasa a OCUPADA/VENDIDA. Pantalla operaciones.xhtml con alta + detalle.

**Archivos:** Operacion (entidad), OperacionService.crear, OperacionBean, operaciones.xhtml, PersonaConverter/ActivoConverter, V18.

**Arquitectura:** BD-centrica (doc 10): toda la matematica financiera vive en V16/V17 como triggers y funciones PL/pgSQL; los services Java las INVOCAN (SELECT f_...), jamas recalculan cuadres.

**Comandos probados:** mvn -q clean package (multi-modulo EXIT 0); deploy-vps.ps1; bateria SQL de 9 checks + E2E (operacion 10 cuotas -> cobro 2M cancela 2 cuotas + caja -> anular repone) todos OK en la VPS.
