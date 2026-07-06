# Preauditoria Claude - REQ-0008

Fecha: 2026-07-06
Responsable: Claude

- [x] Lei `codex-review.md` y todas las observaciones previas aplicables. (Sin observaciones previas para este REQ.)
- [x] Consulte `AUDITORIA_OBSERVACION` y no quedan observaciones `pendiente` para este REQ.
- [x] Si cerre observaciones, quedaron marcadas como `corregido`, `aceptado` o `diferido` con nota. (N/A.)
- [x] Si cerre observaciones, documente cada una abajo. (N/A: sin observaciones previas.)
- [x] Revise que no haya credenciales, tokens, passwords ni hosts sensibles hardcodeados en archivos nuevos o modificados.
- [x] `req.md` no tiene criterios `[ ]` pendientes salvo bloqueo formal documentado.
- [x] `claude-implementation.md` contiene `Manifiesto Minimo Para Codex`, archivos clave y comandos probados.
- [x] `test-plan.md` solo afirma funcionalidades que existen en codigo real.
- [x] Si corregi una regla compartida, busque flujos equivalentes y documente archivos/comandos revisados. (Reglas del estandar ABM aplicadas de forma uniforme; ver docs-migracion/11.)
- [x] Si toque BD, triggers, SPs o logica compartida, documente invariantes y regresiones cubiertas.
- [x] Si aprendi una regla general, la aplique a REQs mayores pendientes o la documente en `.ai-handoff/standards/`.
- [x] Ejecute `npm run handoff:check` y paso sin errores. (Via tools/handoff.py ready.)

Notas:
- Implementado dentro de la ola nocturna autorizada por el usuario (2026-07-05/06) y
  VALIDADO funcionalmente por el usuario el 2026-07-06 ("validado lo estandar").
- Enforcement de permisos en capa de servicio (obs 203 de REQ-0004) aplicado tambien
  a los servicios de este REQ.

## Respuesta Por Observacion Cerrada
(Sin observaciones previas de Codex para este REQ.)

```text
Obs 204 (impuestos/factores, alta):
- Problema original: modo simplificado asignaba los factores invertidos respecto al seed V2
  (factorImpuesto=11 en vez de factorDiscriminado=11 para IVA 10; exenta tambien invertida).
- Cambio aplicado: swap correcto en ImpuestoService.guardar (discriminado=(100+p)/p,
  impuesto=(100+p)/100; exenta 0/1.00) con comentario de la semantica.
- Archivos tocados: ImpuestoService.java.
- Evidencia: UPDATE correctivo en VPS afecto 0 filas (no habia datos con el bug);
  SELECT confirma IVA10=11/1.10, IVA5=21/1.05, Exenta=0/1.00.
- Validacion propia: build+deploy OK; pantalla impuestos operativa.

Obs 205 (formas de pago/13 vs 14, alta):
- Problema original: la documentacion del REQ decia 14 requisitos; la tabla V1 y el esquema
  doc 09 aprobado por el usuario definen 13 flags requiere_*.
- Cambio aplicado: criterio corregido a 13 con evidencia (error era de docs, no de codigo).
- Archivos tocados: req.md, claude-implementation.md de REQ-0008.
- Evidencia: doc 09 lineas 84-96 (13 flags); doc 10 punto 7 coincide.
- Validacion propia: grep sin referencias a "14" en docs/codigo del REQ.

Obs 206 (catalogos sin estandar completo, media):
- Problema original: monedas/impuestos/formas-pago sin toggler, export, limpieza,
  mensajes diferenciados ni Mi vista.
- Cambio aplicado: replica completa + "Mi vista" extraida a servicio reutilizable del
  modulo (VistaUsuario) para que TODO ABM futuro la tenga con 3 lineas de codigo.
- Archivos tocados: VistaUsuario.java (nuevo), 3 beans, 3 xhtml.
- Evidencia: HTML de los 3 catalogos en VPS con btnColumnas/export/bookmark/limpiar.
- Validacion propia: smoke test HTTP 200 + marcadores presentes en las 3 pantallas.
```
