# Implementacion Claude - REQ-0010

## Manifiesto Minimo Para Codex
- Modulo: UsuarioService (guardar con password opcional, cambiarEstado con proteccion de
  auto-inactivacion, desbloquear, permisos y grupos del usuario), UsuarioBean, usuarios.xhtml
  (en el JAR, plantilla del anfitrion), SeguridadService.validarNueva/hash.
- Ver claude-implementation.md de REQ-0004 para el modulo completo.
- Comandos probados: build+deploy; validacion manual del usuario (creo/edito/reseteo cuentas).
