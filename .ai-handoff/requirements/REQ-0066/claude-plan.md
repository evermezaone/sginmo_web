# REQ-0066 - Plan De Implementacion

**Estado:** APROBADO_PARA_DESARROLLO
**Fecha:** 2026-07-12

## Estrategia

Script de restore/simulacro en `tools/vps/` que reconstruye un backup en una base TEMPORAL
(nunca prod, salvo confirmacion doble), valida integridad (Flyway + conteos de tablas criticas con
app.tenant=-1) y emite un reporte de simulacro. Runbook `docs/operacion/restore.md` con RPO/RTO,
recuperacion total, parcial y camino de rollback.

## Archivos A Modificar

| Archivo | Cambio |
|---|---|
| tools/vps/sginmo-restore.sh | NUEVO — restore a base temporal + validaciones + reporte |
| docs/operacion/restore.md | NUEVO — runbook (RPO/RTO, total, parcial, rollback) |

## Pruebas Previstas

- [ ] `bash -n` + modo plan en la VPS (carga config, elige el ultimo dump)
- [ ] Guardia anti-prod (aborta sin --prod-confirm)
- [ ] Simulacro real en base temporal (queda a operaciones por sandbox)

## Riesgos

- El simulacro crea/borra una base temporal en el host de PostgreSQL (escritura): queda a operaciones.
- Restaurar sobre prod es peligroso: se exige confirmacion doble (`--yes` + `--prod-confirm`).

## Cambios De Datos

Sin cambios de esquema. El simulacro opera sobre una base temporal aislada.
