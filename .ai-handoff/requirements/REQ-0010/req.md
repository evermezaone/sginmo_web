# REQ-0010 - Usuarios: ABM, perfiles y contrasenas

**Numero:** REQ-0010 · **Estado:** implementado y validado por el usuario (2026-07-06)

## Objetivo Funcional
Administracion completa de cuentas: alta/edicion con perfil, email y telefono, reseteo de
contrasena, desbloqueo, membresias de grupos y permisos directos. (La autenticacion y el
modelo de permisos son de REQ-0004; este REQ cubre la pantalla de administracion.)

## Criterios De Aceptacion
- [x] ABM usuarios (usuarios.xhtml del modulo ONEsystem-security, integrado a la plantilla
      del anfitrion): pestanas Datos / Grupos / Permisos directos.
- [x] Alta con contrasena inicial (minimo 8) que fuerza cambio al primer ingreso.
- [x] Reseteo por administrador: fuerza cambio, limpia intentos y bloqueo; el choque de
      version optimista da mensaje claro (bug de guardado silencioso corregido).
- [x] Desbloqueo manual; no auto-inactivacion; etiquetas BLOQUEADO / DEBE CAMBIAR CLAVE.
- [x] Enforcement en servicio: UsuarioService.exigir('usuarios', accion) en las 7 escrituras.
