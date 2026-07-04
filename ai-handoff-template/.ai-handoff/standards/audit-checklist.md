# Checklist De Auditoria Y Entrega

Antes de enviar un REQ a Codex:

## General

- `req.md` actualizado con objetivo y criterios claros.
- `claude-implementation.md` incluye manifiesto minimo.
- `preaudit-checklist.md` existe, tiene `Responsable: Claude` y no conserva items `- [ ]`.
- `AUDITORIA_OBSERVACION` no tiene filas `pendiente` para el REQ.
- Toda observacion previa fue marcada como `corregido`, `aceptado` o `diferido` con nota.
- Cada observacion previa cerrada tiene evidencia puntual en `preaudit-checklist.md` o `test-plan.md`: problema original, cambio aplicado, archivos tocados, evidencia y validacion propia.
- Archivos clave listados.
- Comandos ejecutados y resultado documentado.
- Si hubo cambios BD, SQL incluido.

## Revision Transversal De Flujos Equivalentes

**REGLA: corregir un caso no alcanza si existe el mismo patron en otro flujo.**

- Identificar la entidad compartida afectada: tabla, estado, endpoint, helper, componente, SP o trigger.
- Buscar usos equivalentes en todo el proyecto con `rg` u otra herramienta disponible.
- Revisar todos los archivos que insertan/actualizan la misma tabla o ejecutan el mismo efecto de negocio.
- Documentar en `test-plan.md` o `preaudit-checklist.md` la busqueda realizada, archivos revisados y resultado.
- Si algun flujo equivalente queda fuera de alcance, marcarlo como `DIFERIDO` con justificacion y decision del usuario o del REQ.

## Test-plan — Precision Obligatoria

**REGLA: Cada caso de prueba en test-plan.md debe estar respaldado por codigo existente.**

- Verificar que cada boton/endpoint/feature mencionado existe en el archivo `.tsx` o `.py` correspondiente.
- Si una funcionalidad del req.md NO fue implementada: documentarla en la seccion "Excepciones Formales" como DIFERIDO con justificacion.
- NUNCA escribir "aparece boton X" o "genera factura Y" si ese boton/endpoint no existe en el codigo.
- Los criterios `[ ]` sin marcar en `claude-implementation.md` deben tener excepcion formal en test-plan.md.
- Codex rechaza test-plans que afirmen funcionalidades no implementadas. Este tipo de hallazgo requiere ronda extra de correcion.

## Backend

- Endpoints compilan/importan.
- Validaciones criticas estan en backend.
- Permisos backend correctos.
- SQL usa nombres reales de tablas/columnas.
- No hay secrets hardcodeados.
- Errores devuelven mensajes claros.

## Frontend

- `npm run build` OK.
- Rutas en `navigate()` existen en `App.tsx`.
- Menu registrado cuando corresponde.
- Mutaciones invalidan queries relacionadas.
- Botones sensibles no son el unico control de seguridad.
- UI cubre todos los datos exigidos por el criterio.

## Datos

- Migracion idempotente si aplica.
- No hay cambios destructivos sin aprobacion.
- Se considero impacto en datos existentes.
- Stored procedures/triggers existentes no fueron bypassed sin motivo.

## Permisos

- Admin-only validado en backend.
- Permisos activos reconsultan sin cache prolongado.
- Permisos por usuario/grupo respetan semantica definida.
- Anular/editar/crear se verifican segun recurso.

## VB6

- Si el REQ es critico, se documento fuente `.frm`/`.bas`/SP.
- Se listaron reglas heredadas encontradas.
- Se justificaron diferencias.

## Resultado esperado para Codex

Codex debe poder responder:

- `APROBADO_POR_CODEX`: criterios cubiertos y pruebas basicas OK.
- `REQUIERE_CAMBIOS`: hallazgos concretos con archivo/regla.
- `BLOQUEADO_POR_USUARIO`: falta decision de negocio o aprobacion de riesgo.

En una re-auditoria, Codex debe poder revisar primero cada observacion cerrada sin reconstruir todo el contexto desde cero.
