# Preauditoria Claude - REQ-0052

Fecha: 2026-07-12
Responsable: Claude

- [x] Lei `codex-review.md` y observaciones previas. (REQ nuevo, sin observaciones)
- [x] Consulte `AUDITORIA_OBSERVACION` y no quedan observaciones `pendiente` para este REQ.
- [x] Si cerre observaciones, quedaron marcadas con nota. (no aplica)
- [x] Documente observaciones cerradas. (no aplica)
- [x] Revise que no haya credenciales/tokens/passwords/hosts sensibles hardcodeados en archivos nuevos/modificados.
- [x] `req.md` no tiene criterios `[ ]` pendientes (marcados con notas de alcance donde hay refinamientos).
- [x] `claude-implementation.md` contiene Manifiesto Minimo, archivos clave y comandos probados; ademas "Limitaciones Conocidas" explicitas.
- [x] `test-plan.md` solo afirma funcionalidades que existen en codigo real (incluye el bug encontrado y su fix).
- [x] Revise flujos equivalentes: patron de tabla+RLS (V29) y patron ORDEN `Map.of` (ActivoService). Documentado en test-plan.
- [x] Toque BD (tabla nueva + RLS + INSERT nativo): documente invariantes (dedup por indice unico, aislamiento por tenant, sin generacion en contexto global).
- [x] Regla general aprendida: `Map.of(...)` inmutable lanza NPE con clave null en get/getOrDefault; guardar el null. Aplicable a servicios con ORDEN.
- [x] Ejecute `python tools/handoff.py check SGI REQ-0052` y paso sin errores.

Notas:

- Riesgo medio: tabla de negocio nueva con RLS propia + escritura por tenant. Auditor: revisar RLS y dedup.
- Limitaciones documentadas (selectores UI de vinculo, filtros responsable/fecha, promesas/sucursal) como refinamientos/diferidos, no como bloqueos.

## Respuesta Por Observacion Cerrada

(No aplica: REQ nuevo sin observaciones previas.)
