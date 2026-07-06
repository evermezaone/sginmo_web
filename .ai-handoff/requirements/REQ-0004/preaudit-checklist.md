# Preauditoria Claude - REQ-0004

Fecha: 2026-07-06
Responsable: Claude

Antes de ejecutar `npm run handoff:ready -- REQ-0004`, completar todo:

- [x] Lei `codex-review.md` y todas las observaciones previas aplicables.
- [x] Consulte `AUDITORIA_OBSERVACION` y no quedan observaciones `pendiente` para este REQ.
- [x] Si cerre observaciones, quedaron marcadas como `corregido`, `aceptado` o `diferido` con nota. (No hubo observaciones de Codex para este REQ.)
- [x] Si cerre observaciones, documente cada una abajo con problema original, cambio aplicado, archivos tocados, evidencia y validacion propia. (N/A: sin observaciones previas.)
- [x] Revise que no haya credenciales, tokens, passwords ni hosts sensibles hardcodeados en archivos nuevos o modificados. (Credenciales en `.env` gitignorado y en `parametro_sistema` de la BD; SMTP_CLAVE aun vacia; el hash bcrypt del seed V5 es hash, no clave.)
- [x] `req.md` no tiene criterios `[ ]` pendientes salvo bloqueo formal documentado.
- [x] `claude-implementation.md` contiene `Manifiesto Minimo Para Codex`, archivos clave y comandos probados.
- [x] `test-plan.md` solo afirma funcionalidades que existen en codigo real.
- [x] Si corregi una regla compartida, busque flujos equivalentes y documente archivos/comandos revisados. (Regla "referencias absolutas desde naming containers" aplicada a articulos, usuarios y grupos; regla equals/hashCode por id aplicada a todas las entidades.)
- [x] Si toque BD, triggers, SPs o logica compartida, documente invariantes y regresiones cubiertas. (V4-V12 aplicadas a mano en VPS; invariantes: version optimista en 36 tablas, permisos efectivos = directos UNION grupos ACTIVOS, OPERAR excluye VER_AUDITORIA.)
- [x] Si aprendi una regla general, la aplique a REQs mayores pendientes o la documente en `.ai-handoff/standards/`. (Estudio completo del estandar ABM en docs-migracion/11-estandar-abm-propuesta.md, validado por el usuario 2026-07-06.)
- [x] Ejecute `npm run handoff:check` y paso sin errores. (Via tools/handoff.py ready, que lo ejecuta como gate.)

Notas:

- El modulo quedo extraido como JAR reutilizable ONEsystem-security (decision del usuario);
  el detalle de arquitectura esta en claude-implementation.md.
- Validacion FUNCIONAL del usuario (2026-07-06): "validado lo estandar" — probo login,
  permisos, ABM usuarios (reseteo de clave incluido), grupos y los ABMs de catalogo.

## Respuesta Por Observacion Cerrada

```text
Obs 202 (build/compilacion, alta):
- Problema original: build multi-modulo fallaba en onesystem-security por equals con
  "instanceof Tipo variable" (pattern matching Java 16+) en Grupo, PermisoGrupo,
  PermisoUsuario, PreferenciaUsuario, Usuario y UsuarioGrupo.
- Cambio aplicado: instanceof+cast clasico en las 12 entidades (6 del modulo y 6 de
  sginmo, por consistencia); maven-compiler-plugin 3.13.0 con <release>21</release>
  explicito en pluginManagement del parent (toolchain reproducible).
- Archivos tocados: 12 entidades + Desarrollo/pom.xml.
- Evidencia: mvn -q clean package desde Desarrollo EXIT 0; deploy en VPS OK.
- Validacion propia: login + 10 pantallas HTTP 200 con sesion admin tras el deploy.

Obs 203 (seguridad/autorizacion-backend, alta):
- Problema original: permisos por accion solo en beans JSF (sesion.puede); los servicios
  transaccionales de escritura no validaban -> riesgo de invocacion interna sin control.
- Cambio aplicado: nuevo py.com.one.security.servicio.Autorizacion con exigir(pantalla,
  accion) que resuelve SesionUsuario via CDI y lanza NegocioException sin permiso;
  27 llamadas al inicio de TODAS las escrituras @Transactional de los 9 servicios
  (Articulo, Moneda, Impuesto, FormaPago, Lista, Parametro, Geografia, Usuario, Grupo).
  Sin sesion web (jobs/ETL/tests) se permite como proceso de sistema (mismo criterio que
  el fallback "sistema" de la auditoria); los checks de UI quedan como complemento.
- Archivos tocados: Autorizacion.java (nuevo) + 9 servicios.
- Evidencia: grep autorizacion.exigir = 27 llamadas; build y deploy OK.
- Validacion propia: smoke test post-deploy de las 10 pantallas con admin (HTTP 200);
  flujos de guardado siguen operativos (admin tiene todo implicito).
```
