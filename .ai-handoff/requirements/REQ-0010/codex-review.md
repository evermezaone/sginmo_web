# Codex Review - REQ-0010

Fecha: 2026-07-07
Auditor: codex
Resultado: REQUIERE_CAMBIOS

## Verificacion

- Leidos `req.md`, `claude-implementation.md`, `test-plan.md` y `preaudit-checklist.md`.
- Revisados:
  - `Desarrollo/onesystem-security/src/main/java/py/com/one/security/servicio/UsuarioService.java`
  - `Desarrollo/onesystem-security/src/main/java/py/com/one/security/servicio/SeguridadService.java`
  - `Desarrollo/onesystem-security/src/main/java/py/com/one/security/servicio/Autorizacion.java`
  - `Desarrollo/onesystem-security/src/main/java/py/com/one/security/web/UsuarioBean.java`
  - `Desarrollo/onesystem-security/src/main/java/py/com/one/security/dominio/Usuario.java`
  - `Desarrollo/onesystem-security/src/main/resources/META-INF/resources/usuarios.xhtml`
  - `Desarrollo/onesystem-security/src/main/resources/db/esquema-referencia.sql`
  - `Desarrollo/sginmo-web/src/main/resources/db/migration/V10__grupos_seguridad.sql`
- Confirmado correcto:
  - `UsuarioService.guardar()` exige `CREAR`/`EDITAR`;
  - alta con password inicial valida minimo 8, guarda bcrypt y marca `debeCambiarPassword`;
  - reseteo por password no vacio fuerza cambio, limpia intentos y bloqueo;
  - `cambiarEstado()` protege auto-inactivacion;
  - `desbloquear()`, agregar/quitar grupo y agregar/eliminar permiso tienen enforcement en servicio;
  - `SeguridadService` mantiene bcrypt y bloqueo por intentos.

## Hallazgos Bloqueantes

### Obs 209 - El ABM de usuarios no implementa modo solo lectura real

Problema: La grilla usa icono de ojo cuando el usuario tiene `VER` pero no `EDITAR`, pero `UsuarioBean` no tiene estado `soloLectura` y `usuarios.xhtml` no deshabilita campos ni oculta acciones de escritura dentro del dialogo. Un usuario con solo `VER` puede abrir el detalle con inputs editables, ver el boton Guardar, ver controles de agregar/quitar grupos y permisos, e intentar ejecutarlos. El backend bloquea via `UsuarioService.autorizacion.exigir`, pero varias acciones del bean (`desbloquear`, `eliminarPermiso`, `quitarGrupo`) no capturan `NegocioException`, por lo que el flujo puede terminar en error tecnico/no controlado en vez de consulta limpia.

Impacto: Para una pantalla sensible de seguridad, el modo consulta queda incoherente y riesgoso: expone controles de administracion a usuarios sin permiso, genera falsas expectativas de guardado y puede mostrar errores crudos ante acciones no autorizadas. Ademas incumple el estandar ABM ya aplicado en catalogos: mismo ABM en modo solo lectura sin botones de escritura.

Solucion esperada: Implementar `soloLectura = !sesion.puede("usuarios","EDITAR")` al abrir un usuario existente y aplicarlo en `usuarios.xhtml`: deshabilitar inputs/selects/password cuando corresponda, ocultar Guardar y botones de agregar/quitar grupos/permisos, y mantener visible solo consulta. Las acciones de bean que llaman servicios transaccionales deben capturar `NegocioException` y mostrar mensaje controlado. Mantener enforcement en `UsuarioService` como ultima barrera.

Evidencia:
- `Desarrollo/onesystem-security/src/main/resources/META-INF/resources/usuarios.xhtml:62` muestra ojo/lapiz segun permiso.
- `Desarrollo/onesystem-security/src/main/resources/META-INF/resources/usuarios.xhtml:102` y siguientes campos editables no tienen `disabled`.
- `Desarrollo/onesystem-security/src/main/resources/META-INF/resources/usuarios.xhtml:149`, `:162`, `:197`, `:213`, `:225` muestran acciones de escritura sin condicion de solo lectura.
- `Desarrollo/onesystem-security/src/main/java/py/com/one/security/web/UsuarioBean.java` no define `soloLectura`.
- `UsuarioService` si tiene enforcement backend, por lo que el problema es UI/flujo y manejo de excepciones, no ausencia de autorizacion final.

## Pruebas

- Revision estatica de service, bean, entidad y XHTML.
- No se ejecuto un build especifico de cierre porque el REQ queda bloqueado por inspeccion funcional. El build multi-modulo ejecutado en la ronda anterior del mismo ciclo termino EXIT 0.

