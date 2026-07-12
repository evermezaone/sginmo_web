# REQ-0066 - Restore probado, simulacro de recuperacion y runbook operativo

**Numero:** REQ-0066
**Fecha de creacion:** 2026-07-12
**Estado inicial:** NUEVO
**Prioridad:** alta

## Texto Original

Proponer requerimientos para asegurar backup, recuperacion y funcionalidades vendibles.

## Objetivo Funcional

Agregar un procedimiento reproducible de recuperacion ante desastre, con restore probado sobre una base temporal, verificacion de integridad y runbook claro para recuperar SGInmo en la VPS.

## Criterios De Aceptacion

- [ ] Existe script versionado para restaurar un backup en base temporal sin pisar produccion.
- [ ] El restore ejecuta validaciones minimas: conexion, conteos por tablas criticas, Flyway, usuarios, empresas, operaciones, cuotas, cobros, documentos y parametros.
- [ ] El procedimiento genera reporte de simulacro con fecha, backup usado, resultado, errores y duracion.
- [ ] Existe runbook `docs/operacion/restore.md` con pasos de recuperacion total y parcial.
- [ ] El runbook incluye como detener WildFly, resguardar estado actual, restaurar BD, restaurar archivos, iniciar servicio y validar login.
- [ ] El sistema define RPO y RTO objetivo iniciales para cliente chico/mediano.
- [ ] La restauracion exige confirmacion explicita antes de tocar produccion.
- [ ] El proceso no expone secretos en logs ni en documentacion.
- [ ] Se documenta prueba real de restore ejecutada al menos una vez en entorno temporal.

## Reglas De Negocio

- Backup sin restore probado no cuenta como estrategia valida de recuperacion.
- Toda migracion de BD riesgosa debe poder precederse por backup y tener camino de rollback documentado.
- Los datos de cobros, cuotas, operaciones, caja, liquidaciones y documentos son tablas criticas.

## Dependencias

- Depende de: REQ-0065.
- Requerido por: mantenimiento productivo y ventas a clientes que exijan continuidad operativa.

## Fuentes Y Trazabilidad

- Decision usuario 2026-07-11: asegurar backup y recuperacion.
- Estan implicadas reglas de datos criticos de SGInmo: montos, cuotas, cobros, liquidaciones y documentos.
