# Test Plan - REQ-0004 (Seguridad)

Fecha: 2026-07-06. Todas las pruebas ejecutadas contra la VPS (77.237.235.69:8080)
por curl con sesiones reales, mas validacion manual del usuario desde su celular.

## Autenticacion
- [x] GET de cualquier *.xhtml sin sesion -> 302 a login.xhtml (probado: articulos).
- [x] POST AJAX sin sesion -> partial-response con redirect (FiltroAutenticacion).
- [x] Login valido (admin) -> redirect a index; sesion valida para las 10 pantallas (HTTP 200).
- [x] Login con clave erronea -> mensaje generico e intentos_fallidos incrementado en BD
      (verificado; reset posterior).
- [x] Usuario con debe_cambiar_password -> login redirige a cambiar-password y el filtro
      bloquea las demas pantallas hasta cambiarla (probado con consulta reseteada).

## Permisos
- [x] Usuario con solo VER en articulos: sin Nuevo, sin exportar, sin inactivar, icono ojo,
      dialogo en modo consulta, columnas de auditoria AUSENTES (conteo por HTML = 0).
- [x] Usuario con OPERAR en articulos: Nuevo=1, lapiz=10, duplicar=10, export=1,
      auditoria=0 (OPERAR nunca incluye VER_AUDITORIA).
- [x] Usuario sin VER en usuarios -> GET usuarios.xhtml responde 302 a index (viewAction).
- [x] Permisos por grupo: grupo OPER_ARTICULOS con articulos:OPERAR se suma al integrante
      (union en SeguridadService.permisosDe).
- [x] Admin implicito: ve todo incl. columnas de auditoria (conteo por HTML = 2).

## ABM usuarios / grupos
- [x] Alta de usuario con clave inicial -> flag debe_cambiar_password=true.
- [x] Reseteo de clave por admin -> hash nuevo + flag true (falla de version optimista
      ahora da mensaje claro; antes fallaba en silencio — bug corregido).
- [x] No se puede inactivar el propio usuario (regla en UsuarioService).
- [x] Desbloqueo manual limpia intentos y bloqueado_hasta.
- [x] Validado manualmente por el usuario (creo/edito usuarios y grupos desde el ABM).

## Alertas por correo
- [x] Sin SMTP_HOST/clave configurados el envio se omite en silencio y el login no se
      afecta (CorreoService.enviar retorna temprano; enviarAsync captura excepciones).
- [ ] Envio real: BLOQUEO FORMAL documentado — falta que el usuario cargue SMTP_CLAVE
      (pantalla Parametros); el flujo de disparo (intento fallido/bloqueo) ya esta activo.

## Modulo reutilizable
- [x] Pantallas servidas DESDE el JAR (META-INF/resources) responden 200.
- [x] Paginas del modulo integradas a la plantilla del anfitrion (obs de validacion del
      usuario corregida: usuarios/grupos abren dentro del menu).
