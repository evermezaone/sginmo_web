# Preauditoria Claude - REQ-0009

Fecha: 2026-07-06
Responsable: Claude

- [x] Lei `codex-review.md` y todas las observaciones previas aplicables. (Sin observaciones previas; reglas de rondas anteriores aplicadas: instanceof clasico, enforcement en servicios, referencias absolutas.)
- [x] Consulte `AUDITORIA_OBSERVACION` y no quedan observaciones `pendiente` para este REQ.
- [x] Si cerre observaciones, quedaron marcadas con nota. (N/A.)
- [x] Si cerre observaciones, documente cada una abajo. (N/A.)
- [x] Revise que no haya credenciales, tokens, passwords ni hosts sensibles hardcodeados.
- [x] `req.md` no tiene criterios `[ ]` pendientes salvo bloqueo formal documentado.
- [x] `claude-implementation.md` contiene `Manifiesto Minimo Para Codex`, archivos clave y comandos probados.
- [x] `test-plan.md` solo afirma funcionalidades que existen en codigo real.
- [x] Si corregi una regla compartida, documente flujos equivalentes revisados. (Estandar ABM uniforme.)
- [x] Si toque BD, documente invariantes. (Migraciones aplicadas a mano en VPS; version+auditoria en toda tabla.)
- [x] Si aprendi una regla general, la documente. (docs-migracion/11.)
- [x] Ejecute el gate handoff (tools/handoff.py ready) y paso sin errores.

## Respuesta Por Observacion Cerrada
(Sin observaciones previas de Codex para este REQ.)

## Ronda 2 (2026-07-06)
```text
Obs 207 (persistencia edicion empresa, alta):
- Problema: PersonaJuridica.persona con cascade PERSIST solamente; em.merge(empresa) en
  edicion no persistia los datos base de persona (RUC/DV/telefono/direccion/email).
- Cambio: CascadeType.MERGE agregado al @OneToOne @MapsId de PersonaJuridica (y PersonaFisica).
- Archivos: PersonaJuridica.java, PersonaFisica.java.
- Evidencia: edicion UI de empresa existente (telefono='021-999999') -> BD persona.telefono
  actualizado + usuario_modificacion='admin' (antes NULL). Deploy verificado.

Obs 208 (V13 no idempotente, alta):
- Problema: ALTER TABLE ADD COLUMN sin IF NOT EXISTS e INSERT de PANTALLAS sin guarda.
- Cambio: V13 idempotente + los 13 archivos de migracion con el mismo patron (ADD COLUMN
  IF NOT EXISTS; ON CONFLICT DO NOTHING en inserts de entidad/parametro_sistema).
- Archivos: V13 + V2/V4/V5/V7/V8/V10/V11/V12/V14/V15/V18/V20/V21.
- Evidencia: V13 y V12 corridas 2x en transaccion en la VPS sin error. Flyway baseline V21 (REQ-0032).
```
